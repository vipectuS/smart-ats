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

export interface AdminOrganization {
  id: string
  name: string
  enabled: boolean
  tokenPreview: string
  hrCount: number
  jobCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminOrganizationTokenResponse {
  organization: AdminOrganization
  generatedToken: string
}

export interface AdminOrganizationCreatePayload {
  name: string
}

export interface AdminOrganizationUpdatePayload {
  name: string
  enabled: boolean
}

export type AdminParseFailureReviewStatus =
  | 'UNREVIEWED'
  | 'NEEDS_CANDIDATE_UPDATE'
  | 'APPROVED_FOR_RETRY'
  | 'NO_FURTHER_ACTION'

export type AdminParseFailureReviewActionType = 'REVIEW_SAVED' | 'RETRY_QUEUED'

export interface AdminParseFailure {
  resumeId: string
  ownerUsername: string | null
  sourceFileName: string | null
  rawContentReference: string
  parseFailureCode?: string | null
  reason: string | null
  adminReviewNote: string | null
  reviewStatus: AdminParseFailureReviewStatus
  reviewedByUsername: string | null
  reviewedAt: string | null
  updatedAt: string
}

export interface AdminParseFailureSummary {
  totalFailures: number
  firstFailureAt: string | null
  lastFailureAt: string | null
  reviewStatusCounts: AdminDistributionItem[]
  failureCodeCounts: AdminDistributionItem[]
}

export interface AdminParseFailureReviewEvent {
  id: string
  resumeId: string
  adminUsername: string
  actionType: AdminParseFailureReviewActionType
  note: string | null
  previousReviewStatus: AdminParseFailureReviewStatus
  nextReviewStatus: AdminParseFailureReviewStatus
  resumeStatusAfterAction: string
  createdAt: string
}

export interface AdminParseFailureBatchActionResponse {
  processedCount: number
  resumeIds: string[]
  reviewStatus: AdminParseFailureReviewStatus | null
  queued: boolean
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