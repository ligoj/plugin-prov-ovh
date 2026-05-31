# plugin-prov-ovh — Vue UI

Tool-level, i18n-only plugin (`service:prov:ovh`), the OVH provider for the
`prov` service. Compiled to `webjars/prov-ovh/vue/`.

The legacy `ovh.js` contained only dead copy-pasted AWS code and the `prov`
parent has no delegation hook, so this plugin ships only the OVH credential
parameter labels (`service:prov:ovh:*`). `requires: ['prov']`.

```bash
npm install && npm run build && npm run lint && npm test
```
