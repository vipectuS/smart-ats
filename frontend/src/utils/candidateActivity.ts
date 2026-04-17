const CANDIDATE_ACTIVITY_UPDATED_EVENT = 'candidate-activity-updated';

export const notifyCandidateActivityUpdated = () => {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(CANDIDATE_ACTIVITY_UPDATED_EVENT));
};

export const subscribeCandidateActivityUpdated = (listener: () => void) => {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(CANDIDATE_ACTIVITY_UPDATED_EVENT, listener);
  return () => window.removeEventListener(CANDIDATE_ACTIVITY_UPDATED_EVENT, listener);
};