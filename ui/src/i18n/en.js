// OVH parameter labels for the prov-ovh tool plugin. The legacy `ovh.js`
// controller had dead copy-pasted AWS code; the real surface is the four
// OVHcloud API credential parameters declared in parameter.csv (and the
// legacy nls bundle). i18n is the entire UI surface (the `prov` parent
// has no delegation hook).
export default {
  'service:prov:ovh:app-key-id': 'Application Key',
  'service:prov:ovh:app-secret': 'Application Secret',
  'service:prov:ovh:consumer-key': 'Consumer Key',
  'service:prov:ovh:service-name': 'Service Name',
}
