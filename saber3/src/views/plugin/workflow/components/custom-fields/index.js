
const requireComponent = import.meta.glob('./**/index.vue', { eager: true })
const globalResult = Object.keys(requireComponent).filter((_) => _)
const install = (app) => {
  globalResult.forEach((item) => {
    const component = requireComponent[item]?.default
    if (!component.name) return
    app.component(component.name.startsWith('wf-') ? component.name : `wf-${component.name}`, component)
  })
}

export default {
  install
}