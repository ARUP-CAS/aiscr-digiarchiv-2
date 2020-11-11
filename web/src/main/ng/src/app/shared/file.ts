export class File {
  nazev: string;
  mimetype: string;
  rozsah: number;
  size_bytes: number;
  pages: any[];
  humanFileSize: string;
  filepath: string;

  setSize(si) {
    var thresh = si ? 1000 : 1024;
    if (Math.abs(this.size_bytes) < thresh) {
      this.humanFileSize = this.size_bytes + ' B';
    }
    var units = si
      ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
      : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    var u = -1;
    do {
      this.size_bytes /= thresh;
      ++u;
    } while (Math.abs(this.size_bytes) >= thresh && u < units.length - 1);
    this.humanFileSize = this.size_bytes.toFixed(1) + ' ' + units[u];
  }
}
