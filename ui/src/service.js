/*
 * Service layer for plugin "prov-ovh". The legacy `ovh.js` contained only
 * dead copy-pasted AWS code (referencing `service:prov:aws:*`), and the
 * `prov` parent has no delegation hook — so this tool's real UI surface
 * is the OVH credential parameter labels shipped via i18n. Kept empty for
 * contract parity.
 */
const service = {}

export default service
