export const JobApplicationStatus = {
  NONE: 'NONE',
  APPLIED: 'APPLIED',
  WITHDRAWN: 'WITHDRAWN',
  FAVORITE: 'FAVORITE',
  IGNORE: 'IGNORE'
} as const;

export type JobApplicationStatus = typeof JobApplicationStatus[keyof typeof JobApplicationStatus];
