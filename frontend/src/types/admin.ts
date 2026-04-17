export interface AdminDistributionItem {
  label: string
  value: number
}

export interface AdminOverviewTotals {
  totalUsers: number
  totalJobs: number
  totalResumes: number
  totalSkillEntries: number
}

export interface AdminParseFailure {
  resumeId: string
  ownerUsername: string | null
  sourceFileName: string | null
  rawContentReference: string
  reason: string | null
  updatedAt: string
}

export interface AdminOverview {
  totals: AdminOverviewTotals
  usersByRole: AdminDistributionItem[]
  resumesByStatus: AdminDistributionItem[]
  latestParseFailures: AdminParseFailure[]
}

export interface AdminSkill {
  id: string
  name: string
  category: string | null
  aliases: string[]
  enabled: boolean
  updatedAt: string
}

export interface AdminSkillUpsertPayload {
  name: string
  category: string | null
  aliases: string[]
  enabled: boolean
}