-- migration_v6_capture_anywhere.sql — "verify anywhere" model (Rudra Sir, 2026-09-16):
-- The QR identifies the partner station; the SCANNER's real GPS position + address is
-- the verification evidence. No distance-based rejection, no daily limit, no device
-- mismatch rejection — everything lands in Admin Review with full evidence and the
-- admin approves/rejects. Partner locations no longer carry fixed coordinates
-- (the Dhaka demo seeds are neutralised; printed QR signatures are unaffected).

DROP FUNCTION IF EXISTS public.submit_qr_verification(text, double precision, double precision, double precision, text, text, text[], text);

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
  v_status text;
  v_note text;
  v_dist double precision;
  v_ripple bigint;
begin
  -- approved accounts only
  select approval_status into v_status
    from public.profiles where id = v_user;
  if not found then raise exception 'profile missing'; end if;
  if coalesce(v_status,'pending') <> 'approved' then
    raise exception 'Your account is awaiting admin verification';
  end if;

  -- the QR must point at a real, active partner station
  select * into v_loc from public.partner_locations where id = p_location and active;
  if not found then raise exception 'Unknown location'; end if;

  -- capture model: the scanner's GPS is evidence, never a rejection reason
  if p_user_lat is null or p_user_lng is null then
    v_status := 'flagged'; v_note := 'No GPS fix provided';
  else
    v_status := 'pending'; v_note := null;
  end if;

  -- informational distance only (when the station row happens to have coordinates)
  if v_loc.lat is not null and v_loc.lng is not null and p_user_lat is not null then
    v_dist := 6371000 * 2 * asin(
      sqrt(
        power(sin(radians(v_loc.lat - p_user_lat) / 2), 2) +
        cos(radians(p_user_lat)) * cos(radians(v_loc.lat)) *
        power(sin(radians(v_loc.lng - p_user_lng) / 2), 2)
      )
    );
  end if;

  -- remember the first device used (informational)
  update public.profiles
    set device_id = coalesce(device_id, p_device)
    where id = v_user;

  -- ripple (awaiting admin review) + verification row with the captured evidence
  insert into public.ripples (user_id, title, subtitle, points, co2e_grams, status, action_key, location_id)
    values (v_user, 'Partner action @ ' || v_loc.name, 'QR-verified at ' || v_loc.name,
            500, 1200, 'pending_review', p_action_key, p_location)
    returning id into v_ripple;

  insert into public.verifications (user_id, ripple_id, location_id, method,
      user_lat, user_lng, user_address, accuracy_m, distance_m, device, status, review_note, photos)
    values (v_user, v_ripple, p_location, 'qr',
            p_user_lat, p_user_lng, p_address, p_accuracy::int, v_dist::int, p_device, v_status, v_note,
            coalesce(p_photos, '{}'::text[]));

  -- points/stance are decided by the admin in approve_verification
  return v_ripple;
end; $function$;

-- Neutralise the Dhaka demo coordinates: stations work anywhere in the world.
-- (Names/emoji/active stay, so already-printed QR codes keep validating.)
UPDATE public.partner_locations
  SET lat = NULL, lng = NULL, radius_m = NULL, address = NULL;

NOTIFY pgrst, 'reload schema';

-- legacy overload with p_user_address (would make PostgREST resolution ambiguous)
DROP FUNCTION IF EXISTS public.submit_qr_verification(p_location text, p_user_lat double precision, p_user_lng double precision, p_accuracy double precision, p_device text, p_action_key text, p_user_address text, p_photos text[]);
