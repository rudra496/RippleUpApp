-- migration_v4_photos.sql — wire proof photos + user address through QR verification.
-- Run via psycopg2 session pooler (dashboard automation not required).

DROP FUNCTION IF EXISTS public.submit_qr_verification(text, double precision, double precision, double precision, text, text);

CREATE OR REPLACE FUNCTION public.submit_qr_verification(
  p_location text,
  p_user_lat double precision DEFAULT NULL,
  p_user_lng double precision DEFAULT NULL,
  p_accuracy double precision DEFAULT NULL,
  p_device text DEFAULT NULL,
  p_action_key text DEFAULT 'refill',
  p_photos text[] DEFAULT NULL,
  p_address text DEFAULT NULL
)
 RETURNS bigint
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
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
      user_lat, user_lng, user_address, accuracy_m, distance_m, device, status, review_note, photos)
    values (v_user, v_ripple, p_location, 'qr',
            p_user_lat, p_user_lng, p_address, p_accuracy::int, v_dist::int, p_device, v_status, v_note,
            coalesce(p_photos, '{}'));

  -- streak/points live in profiles on admin approval (approve_verification)
  return v_ripple;
end; $function$;
