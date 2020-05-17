export interface Sound {
  displayName: string;
  filename: string;
}

export function soundSorter(a: Sound, b: Sound): number {
  if (a.displayName && b.displayName) {
    return a.displayName.localeCompare(b.displayName);
  } else if (a.displayName) {
    return 1;
  } else if (b.displayName) {
    return -1;
  } else {
    return a.filename.localeCompare(b.filename);
  }
}
