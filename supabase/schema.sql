-- ============================================================
-- RippleUp PRODUCTION schema v1
-- Paste into Supabase Dashboard -> SQL Editor -> Run (once).
-- Free tier friendly; RLS everywhere; the app only uses the publishable key.
-- ============================================================
create extension if not exists pgcrypto;

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.profiles where id = auth.uid() and is_admin);
$$;

-- ---------- profiles ----------
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text unique not null,
  full_name text,
  is_admin boolean not null default false,
  total_points integer not null default 0,
  co2e_grams integer not null default 0,
  streak_days integer not null default 0,
  longest_streak integer not null default 0,
  created_at timestamptz not null default now()
);

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, email, full_name)
  values (new.id, new.email,
          coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email,'@',1)));
  return new;
end; $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute procedure public.handle_new_user();

-- ---------- partner locations (where QR codes are placed) ----------
create table if not exists public.partner_locations (
  id text primary key,
  name text not null,
  address text,
  emoji text default 'P',
  lat double precision not null,
  lng double precision not null,
  radius_m integer not null default 150,
  qr_sig text not null,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

-- ---------- ripples (actions) ----------
create table if not exists public.ripples (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  subtitle text,
  points integer not null default 0,
  co2e_grams integer not null default 0,
  status text not null default 'self',
  action_key text not null default 'custom',
  location_id text references public.partner_locations(id),
  created_at timestamptz not null default now()
);
create index if not exists ripples_user_idx on public.ripples(user_id, created_at desc);

-- ---------- verifications (who scanned, from where) ----------
create table if not exists public.verifications (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  ripple_id bigint references public.ripples(id),
  location_id text references public.partner_locations(id),
  method text not null default 'qr',
  user_lat double precision,
  user_lng double precision,
  distance_m integer,
  device text,
  status text not null default 'pending',
  review_note text,
  reviewed_by uuid references auth.users(id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists verif_user_idx on public.verifications(user_id, created_at desc);
create index if not exists verif_status_idx on public.verifications(status, created_at desc);

-- ---------- badges + achievements ----------
create table if not exists public.badges (
  key text primary key,
  title text not null,
  emoji text,
  description text,
  need_count integer not null default 1,
  action_filter text,
  award_points integer not null default 0
);

create table if not exists public.user_badges (
  user_id uuid references auth.users(id) on delete cascade,
  badge_key text references public.badges(key) on delete cascade,
  earned_at timestamptz not null default now(),
  primary key (user_id, badge_key)
);

-- ---------- event registrations + notifications ----------
create table if not exists public.event_registrations (
  id bigint generated always as identity primary key,
  user_id uuid references auth.users(id) on delete cascade,
  event_key text not null,
  created_at timestamptz not null default now(),
  unique (user_id, event_key)
);

create table if not exists public.notifications (
  id bigint generated always as identity primary key,
  user_id uuid references auth.users(id) on delete cascade,
  title text not null,
  body text,
  tone text default 'mint',
  read boolean not null default false,
  created_at timestamptz not null default now()
);

-- ---------- PIPELINES ----------

-- atomic admin approval: verification -> approved, ripple verified, points awarded
create or replace function public.approve_verification(p_verification bigint)
returns void language plpgsql security definer set search_path = public as $$
declare v_ver public.verifications; v_rip public.ripples;
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.verifications
    set status='approved', reviewed_by=auth.uid(), reviewed_at=now()
    where id = p_verification and status in ('pending','flagged')
    returning * into v_ver;
  if v_ver.ripple_id is not null then
    update public.ripples set status='qr' where id = v_ver.ripple_id;
    select * into v_rip from public.ripples where id = v_ver.ripple_id;
    update public.profiles
      set total_points = total_points + coalesce(v_rip.points,0),
          co2e_grams   = co2e_grams   + coalesce(v_rip.co2e_grams,0)
      where id = v_ver.user_id;
    insert into public.notifications (user_id, title, body, tone)
      values (v_ver.user_id, 'Contribution approved!',
              'Your ripple at ' || coalesce(v_ver.location_id,'partner') ||
              ' was verified by our team. +' || coalesce(v_rip.points,0) || ' pts added.', 'mint');
  end if;
end; $$;

-- admin rejection (with note -> notification)
create or replace function public.reject_verification(p_verification bigint, p_note text default null)
returns void language plpgsql security definer set search_path = public as $$
declare v_user uuid;
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.verifications
    set status='rejected', review_note=p_note, reviewed_by=auth.uid(), reviewed_at=now()
    where id = p_verification and status in ('pending','flagged')
    returning user_id into v_user;
  update public.ripples set status='pending'
    where id = (select ripple_id from public.verifications where id = p_verification);
  insert into public.notifications (user_id, title, body, tone)
    values (v_user, 'Verification needs attention',
            'A verification was not approved. Reason: ' || coalesce(p_note,'not specified'), 'cream');
end; $$;

-- achievements pipeline: call after inserting a verified ripple
create or replace function public.sync_badges(p_user uuid default auth.uid())
returns setof text language plpgsql security definer set search_path = public as $$
declare b record; cnt int; pts int;
begin
  select coalesce(total_points,0) into pts from public.profiles where id = p_user;
  for b in select * from public.badges loop
    if b.key = 'ambassador' then cnt := pts;
    else
      select count(*) into cnt from public.ripples
        where user_id = p_user and status in ('self','qr')
        and (b.action_filter is null or action_key = b.action_filter);
    end if;
    if cnt >= b.need_count then
      insert into public.user_badges (user_id, badge_key) values (p_user, b.key)
        on conflict do nothing;
      if found then
        return next b.key;
        insert into public.notifications (user_id, title, body, tone)
          values (p_user, coalesce(b.emoji,'') || ' Badge earned: ' || b.title, b.description, 'mint');
      end if;
    end if;
  end loop;
end; $$;

-- ---------- ROW LEVEL SECURITY ----------
alter table public.profiles enable row level security;
drop policy if exists profiles_read on public.profiles;
create policy profiles_read on public.profiles for select
  using (auth.uid() = id or public.is_admin());
drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles for update
  using (auth.uid() = id) with check (auth.uid() = id);

alter table public.partner_locations enable row level security;
drop policy if exists locations_read on public.partner_locations;
create policy locations_read on public.partner_locations for select
  using (auth.role() = 'authenticated');

alter table public.ripples enable row level security;
drop policy if exists ripples_own on public.ripples;
create policy ripples_own on public.ripples for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists ripples_admin on public.ripples;
create policy ripples_admin on public.ripples for select using (public.is_admin());

alter table public.verifications enable row level security;
drop policy if exists verif_read on public.verifications;
create policy verif_read on public.verifications for select
  using (auth.uid() = user_id or public.is_admin());
drop policy if exists verif_insert on public.verifications;
create policy verif_insert on public.verifications for insert
  with check (auth.uid() = user_id);
drop policy if exists verif_admin_update on public.verifications;
create policy verif_admin_update on public.verifications for update using (public.is_admin());

alter table public.badges enable row level security;
drop policy if exists badges_read on public.badges;
create policy badges_read on public.badges for select using (auth.role() = 'authenticated');

alter table public.user_badges enable row level security;
drop policy if exists ubadges_read on public.user_badges;
create policy ubadges_read on public.user_badges for select using (auth.uid() = user_id or public.is_admin());

alter table public.event_registrations enable row level security;
drop policy if exists events_own on public.event_registrations;
create policy events_own on public.event_registrations for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

alter table public.notifications enable row level security;
drop policy if exists notif_read on public.notifications;
create policy notif_read on public.notifications for select using (auth.uid() = user_id);

-- ---------- SEEDS (sample badges + 3 sample locations) ----------
insert into public.badges (key,title,emoji,description,need_count,action_filter,award_points) values
 ('starter','Ripple Starter','S1','Complete your first action',1,null,50),
 ('first_refill','First Refill','S2','Scan a refill QR at a partner',1,'refill',100),
 ('plastic_saver','Plastic Saver','S3','Avoid 10 plastic bottles',10,'refill',300),
 ('zero_waste','Zero Waste Hero','S4','Complete 5 recycling actions',5,'recycle',150),
 ('champion','Community Champion','S5','Attend 3 community clean-ups',3,'cleanup',200),
 ('ambassador','Ripple Ambassador','S6','Reach 5,000 total points',5000,null,500)
on conflict (key) do nothing;

insert into public.partner_locations (id,name,address,emoji,lat,lng,radius_m,qr_sig) values
  ('loc_greencentre', 'Green Brew Cafe', 'Gulshan-1, Dhaka', 23.7925, 90.4078, 150, '0dc6f57329d951c2'),
  ('loc_ecorecycle', 'EcoRecycle Hub', 'Dhanmondi, Dhaka', 23.7461, 90.3742, 200, '56a4470c410c2eab'),
  ('loc_thriftup', 'ThriftUp Store', 'Banani, Dhaka', 23.7936, 90.4043, 150, '2b04071672d3dcf8')
on conflict (id) do nothing;

-- ============================================================
-- AFTER your first Google/email login, make yourself admin (SQL editor):
--   update public.profiles set is_admin = true where email = 'you@example.com';
--
-- Recommended (free tier): Authentication -> Providers -> disable "Confirm email"
-- while testing. Later buy Supabase Pro and re-enable it.
-- ============================================================
