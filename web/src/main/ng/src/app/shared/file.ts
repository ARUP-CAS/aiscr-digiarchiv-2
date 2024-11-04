export class File {
  id: string;
  nazev: string;
  mimetype: string;
  rozsah: number;
  size_mb: number;
  pages: any[];
  humanFileSize: string;
  filepath: string;

  setSize(si) {
    var thresh = si ? 1000 : 1024;
    if (Math.abs(this.size_mb) < thresh) {
      this.humanFileSize = this.size_mb + ' B';
    }
    var units = si
      ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
      : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    var u = -1;
    do {
      this.size_mb /= thresh;
      ++u;
    } while (Math.abs(this.size_mb) >= thresh && u < units.length - 1);
    this.humanFileSize = this.size_mb.toFixed(1) + ' ' + units[u];
  }
}
