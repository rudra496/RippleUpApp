-- ============================================================
-- RippleUp SECURITY + PRODUCTION migration v2
-- Paste into Supabase SQL Editor -> Run (after schema.sql).
-- Safe to re-run (idempotent).
-- ============================================================

-- 1) ACCOUNT APPROVAL GATE: new accounts wait for admin approval
alter table public.profiles add column if not exists approval_status text not null default 'pending';
alter table public.profiles add column if not exists device_id text;

-- 2) CLOSE SECURITY HOLE: users must never be able to edit is_admin/approval_status/points.
--    Column-level grants: authenticated users may only change their display name.
revoke update on table public.profiles from authenticated;
grant update (full_name) on table public.profiles to authenticated;

-- 3) EVENTS (real, editable by you in Table Editor)
create table if not exists public.events (
  key text primary key,
  emoji text default 'P',
  title text not null,
  date text,
  place text,
  going integer not null default 0,
  points integer not null default 500,
  active boolean not null default true
);
alter table public.events enable row level security;
drop policy if exists events_read on public.events;
create policy events_read on public.events for select using (auth.role() = 'authenticated');

insert into public.events (key, emoji, title, date, place, going, points) values
 ('ev_city_park', 'E1', 'City Park Clean-Up', 'Sat, Sept 22', 'Narendra Park', 92, 500),
 ('ev_plantation', 'E2', 'Tree Plantation Drive', 'Sun, Sept 23', 'GMC Campus', 68, 500)
on conflict (key) do nothing;

-- 4) ANTI-ABUSE: server-side QR verification.
--    The CLIENT cannot fake distance/points anymore — everything is checked here:
--      * account must be approved by admin
--      * location must exist and be active
--      * server computes the real distance; > 2x radius = hard block,
--        1x..2x radius = flagged for admin review, <= radius = pending
--      * max ONE verification per user per location per day
--      * device binding: first device registers; a different device = flagged
create or replace function public.submit_qr_verification(
  p_location text,
  p_user_lat double precision default null,
  p_user_lng double precision default null,
  p_accuracy double precision default null,
  p_device text default null,
  p_action_key text default 'refill'
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := auth.uid();
  v_loc public.partner_locations;
  v_dist double precision;
  v_status text;
  v_note text;
  v_ripple bigint;
  v_device text;
begin
  -- approved accounts only
  select approval_status, device_id into v_status, v_device
    from public.profiles where id = v_user;
  if not found then raise exception 'profile missing'; end if;
  if coalesce(v_status,'pending') <> 'approved' then
    raise exception 'Your account is awaiting admin verification';
  end if;

  -- location
  select * into v_loc from public.partner_locations where id = p_location and active;
  if not found then raise exception 'Unknown location'; end if;

  -- one verification per user per location per day
  if exists (
    select 1 from public.verifications
    where user_id = v_user and location_id = p_location
      and created_at >= date_trunc('day', now())
  ) then
    raise exception 'You already scanned this location today. Come back tomorrow!';
  end if;

  -- server-side distance check
  if p_user_lat is null or p_user_lng is null then
    v_status := 'flagged'; v_note := 'No GPS fix provided'; v_dist := null;
  else
    v_dist := 6371000 * 2 * asin(
      sqrt(
        power(sin(radians(v_loc.lat - p_user_lat) / 2), 2) +
        cos(radians(p_user_lat)) * cos(radians(v_loc.lat)) *
        power(sin(radians(v_loc.lng - p_user_lng) / 2), 2)
      )
    );
    if v_dist <= v_loc.radius_m then
      v_status := 'pending'; v_note := null;
    elsif v_dist <= v_loc.radius_m * 2 then
      v_status := 'flagged'; v_note := 'Slightly outside the location radius';
    else
      raise exception 'You are too far from % to verify here (distance: % m)', v_loc.name, v_dist::int;
    end if;
  end if;

  -- device binding
  if v_note is null and v_device is not null and coalesce(v_device, '') <> coalesce(p_device, '') then
    v_status := 'flagged'; v_note := 'Scanned from a different device than usual';
  end if;
  if v_device is null then
    update public.profiles set device_id = p_device where id = v_user;
  end if;

  -- ripple (awaiting review) + verification row
  insert into public.ripples (user_id, title, subtitle, points, co2e_grams, status, action_key, location_id)
    values (v_user, 'Partner action @ ' || v_loc.name, 'QR-verified at ' || v_loc.name,
            500, 1200, 'pending_review', p_action_key, p_location)
    returning id into v_ripple;

  insert into public.verifications (user_id, ripple_id, location_id, method,
      user_lat, user_lng, user_address, accuracy_m, distance_m, device, status, review_note)
    values (v_user, v_ripple, p_location, 'qr',
            p_user_lat, p_user_lng, null, p_accuracy::int, v_dist::int, p_device, v_status, v_note);

  -- streak/points live in profiles on admin approval (approve_verification)
  return v_ripple;
end; $$;

-- 5) ADMIN: approve / reject new user accounts
create or replace function public.set_user_approval(p_user uuid, p_status text)
returns void
language plpgsql security definer set search_path = public as $$
declare v_email text;
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  if p_status not in ('approved','rejected','pending') then raise exception 'bad status'; end if;
  update public.profiles set approval_status = p_status where id = p_user;
  select email into v_email from public.profiles where id = p_user;
  if p_status = 'approved' then
    insert into public.notifications (user_id, title, body, tone)
      values (p_user, 'Account verified!', 'Welcome to RippleUp — your account was approved. Start making ripples!', 'mint');
  elsif p_status = 'rejected' then
    insert into public.notifications (user_id, title, body, tone)
      values (p_user, 'Account not approved', 'Your account needs more verification. Contact support.', 'cream');
  end if;
end; $$;

-- 6) streak helper (consecutive days ending today with verified ripples)
create or replace function public.current_streak(p_user uuid)
returns integer
language sql stable security definer set search_path = public as $$
  with days as (
    select distinct date(created_at) as d
    from public.ripples
    where user_id = p_user and status in ('self','qr')
  ),
  ranked as (
    select d, d - (row_number() over (order by d))::int as grp
    from days
    where d <= current_date
  )
  select coalesce(max(cnt), 0) from (
    select count(*) as cnt, grp from ranked group by grp
      having bool_or(d = current_date or d = current_date - 1)
  ) x;
$$;

-- 7) make registration approval instant for YOU after your first OTP login:
--    update public.profiles set is_admin = true, approval_status = 'approved'
--    where email = 'rudrasarker131@gmail.com';
