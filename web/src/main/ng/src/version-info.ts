export const versionInfo = (() => {
  try {
     // tslint:disable-next-line:no-var-requires
     return require('../git-version.json');
   } catch {
    // In dev the file might not exist:
    return { hash: 'dev', date: Date.now() };
   }
})();
