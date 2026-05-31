/*
 * Plugin "prov-ovh" — OVH implementation of plugin-prov.
 *
 * Tool-level, i18n-only plugin (`service:prov:ovh`). The legacy `ovh.js`
 * held only dead AWS copy-paste code and the `prov` parent has no
 * delegation hook, so this plugin's only contribution is the OVH
 * credential parameter labels for the subscribe wizard.
 */
import { useI18nStore } from '@ligoj/host'
import enMessages from './i18n/en.js'
import frMessages from './i18n/fr.js'
import service from './service.js'

const features = {}

export default {
  id: 'prov-ovh',
  label: 'OVH',
  requires: ['prov'],
  install() {
    const i18n = useI18nStore()
    i18n.merge(enMessages, 'en')
    i18n.merge(frMessages, 'fr')
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "prov-ovh" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-cloud-outline', color: 'indigo-darken-3' },
}

export { service }
