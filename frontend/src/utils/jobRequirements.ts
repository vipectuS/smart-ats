export interface JobRequirementsInput {
  department?: string;
  location?: string;
  headcount?: number | string;
  seniority?: string;
  employmentType?: string;
  education?: string;
  salaryRange?: string;
  experienceYears?: number | string;
  skills?: string[];
  responsibilitiesText?: string;
  experienceKeywordsText?: string;
  educationKeywordsText?: string;
  highlightsText?: string;
}

export interface JobRequirementsBuildOptions {
  defaultDepartment?: string;
  defaultLocation?: string;
}

const normalizeSkills = (skills: string[] | undefined): string[] => {
  if (!skills) return [];
  const seen = new Set<string>();
  const normalized: string[] = [];

  for (const skill of skills) {
    const value = skill.trim();
    if (!value) continue;
    if (seen.has(value)) continue;
    seen.add(value);
    normalized.push(value);
  }

  return normalized;
};

const normalizeLines = (text: string | undefined): string[] => {
  if (!text) return [];
  return text
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

export const buildJobRequirementsPayload = (
  input: JobRequirementsInput,
  options: JobRequirementsBuildOptions = {},
): Record<string, unknown> => {
  const requirements: Record<string, unknown> = {};

  const department = input.department?.trim() || options.defaultDepartment;
  if (department) requirements.department = department;

  const location = input.location?.trim() || options.defaultLocation;
  if (location) requirements.location = location;

  const parsedHeadcount = Number(input.headcount);
  if (Number.isFinite(parsedHeadcount) && parsedHeadcount > 0) {
    requirements.headcount = parsedHeadcount;
  }

  if (input.seniority?.trim()) requirements.seniority = input.seniority.trim();
  if (input.employmentType?.trim()) requirements.employmentType = input.employmentType.trim();
  if (input.education?.trim()) requirements.education = input.education.trim();
  if (input.salaryRange?.trim()) requirements.salaryRange = input.salaryRange.trim();

  const parsedExperienceYears = Number(input.experienceYears);
  if (Number.isFinite(parsedExperienceYears) && parsedExperienceYears >= 0) {
    requirements.experienceYears = parsedExperienceYears;
  }

  const skills = normalizeSkills(input.skills);
  if (skills.length > 0) requirements.skills = skills;

  const responsibilities = normalizeLines(input.responsibilitiesText);
  if (responsibilities.length > 0) requirements.responsibilities = responsibilities;

  const experienceKeywords = normalizeLines(input.experienceKeywordsText);
  if (experienceKeywords.length > 0) requirements.experienceKeywords = experienceKeywords;

  const educationKeywords = normalizeLines(input.educationKeywordsText);
  if (educationKeywords.length > 0) requirements.educationKeywords = educationKeywords;

  const highlights = normalizeLines(input.highlightsText);
  if (highlights.length > 0) requirements.highlights = highlights;

  return requirements;
};
