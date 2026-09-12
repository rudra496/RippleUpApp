-- migration_v5_photo_retention.sql — proof photos are DELETED from storage the moment
-- an admin approves or rejects the verification (privacy: no photo retention).

CREATE OR REPLACE FUNCTION public.approve_verification(p_verification bigint)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_ver public.verifications; v_rip public.ripples;
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.verifications
    set status='approved', reviewed_by=auth.uid(), reviewed_at=now()
    where id = p_verification and status in ('pending','flagged')
    returning * into v_ver;
  if v_ver.id is null then return; end if;
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
end; $function$;

CREATE OR REPLACE FUNCTION public.reject_verification(p_verification bigint, p_note text DEFAULT NULL::text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare v_user uuid;
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.verifications
    set status='rejected', review_note=p_note, reviewed_by=auth.uid(), reviewed_at=now()
    where id = p_verification and status in ('pending','flagged')
    returning user_id into v_user;
  if v_user is null then return; end if;
  update public.ripples set status='pending'
    where id = (select ripple_id from public.verifications where id = p_verification);
  insert into public.notifications (user_id, title, body, tone)
    values (v_user, 'Verification needs attention',
            'A verification was not approved. Reason: ' || coalesce(p_note,'not specified'), 'cream');
end; $function$;

-- Storage DELETE policy so the admin app can remove proof photos via the Storage API
-- (direct SQL deletes on storage.objects are blocked by Supabase's orphan-protection trigger).
CREATE POLICY "vp admin delete" ON storage.objects FOR DELETE TO authenticated
  USING (bucket_id = 'verification-photos' AND public.is_admin());
