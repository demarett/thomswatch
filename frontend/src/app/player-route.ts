export function toRouteBattleTag(tag: string): string {
  return tag.replace(/#(?=\d+$)/, '-');
}

export function fromRouteBattleTag(tag: string): string {
  return tag.replace(/-(\d+)$/, '#$1');
}
