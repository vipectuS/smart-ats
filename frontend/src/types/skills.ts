export interface PublicSkillCatalogItem {
  name: string;
  aliases: string[];
}

export type SkillOptionInput = string | PublicSkillCatalogItem;
