export class Utils {

  /**
   * Method copies source properties to target only if they exist in target
   * @param source the source object
   * @param target the target
   */

  public static sanitize(source, target) {
    for (const p in source) {
      if (source[p] && p in target) {
        target[p] = source[p];
      }
    }
  }

  public static hasValue(obj: any, field: string): boolean {
    if (obj.hasOwnProperty(field)) {
      if (typeof obj[field] === 'string') {
        return obj[field].trim() !== '';
      } else {
        return obj[field][0].trim() !== '';
      }

    } else {
      return false;
    }
  }

  public static formatJSON(o: any): string {
    return JSON.stringify(o);
  }
}
