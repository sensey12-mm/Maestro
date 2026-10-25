// Observer-owned snapshot. Called ONLY after a whole public registration map
// matches pinned original factory bytes and the same context/dispatch epoch.
// No callback/accessor is invoked and no battle/actor/request is accepted.
(function (registry, ids) {
  let budget = 20000;
  const clone = value => {
    if (--budget < 0) throw Error('native snapshot budget');
    if (value === null || typeof value !== 'object') return value; // Function identity stays exact.
    if (Object.getOwnPropertySymbols(value).length) throw Error('native snapshot symbols');
    const result = Array.isArray(value) ? [] : Object.create(Object.getPrototypeOf(value));
    for (const key of Object.getOwnPropertyNames(value)) {
      const d = Object.getOwnPropertyDescriptor(value, key);
      if (!d || d.get || d.set) throw Error('native snapshot accessor');
      Object.defineProperty(result, key, {...d, value: clone(d.value)});
    }
    return result;
  };
  const d = Object.getOwnPropertyDescriptor(registry, 'contents');
  if (!d || d.get || d.set || !(d.value instanceof Map)
      || Object.getPrototypeOf(d.value) !== Map.prototype || Reflect.ownKeys(d.value).length)
    throw Error('native registry structure');
  const captured = new Map();
  for (const id of ids) {
    if (!/^[a-z0-9]+$/.test(id) || !d.value.has(id)) throw Error('native registry identity');
    captured.set(id, clone(d.value.get(id)));
  }
  return captured;
})
