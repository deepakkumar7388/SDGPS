import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../database/db';
import { GatePassRepository } from '../repositories/GatePassRepository';
import { InterInstitutionalGatePassRepository } from '../repositories/InterInstitutionalGatePassRepository';
import { VisitorRepository } from '../repositories/VisitorRepository';

const EMPTY_ARRAY = [];

// ── Helpers ──────────────────────────────────────────────────────────────────
const todayBounds = () => {
  const today = new Date().toISOString().split('T')[0]; // 'YYYY-MM-DD'
  return {
    start: `${today} 00:00:00`,
    end:   `${today} 23:59:59`,
  };
};

// ── RAW hooks (all records, used by History range filter & MyGatePass) ────────
export const useGatePasses = () =>
  useLiveQuery(() => db.gatePasses.orderBy('applyDate').reverse().toArray(), []) || EMPTY_ARRAY;

export const useInterInstitutionalGatePasses = () =>
  useLiveQuery(() => db.interInstitutionalGatePasses.orderBy('applyDate').reverse().toArray(), []) || EMPTY_ARRAY;

export const useVisitors = () =>
  useLiveQuery(() => db.visitors.orderBy('meetDate').reverse().toArray(), []) || EMPTY_ARRAY;

// ── ACTIVE hooks (today, active statuses) — mirrors Android DAO exactly ──────

/**
 * Gate passes that are ACTIVE TODAY.
 * - Security guard: only 'approved'
 * - Others (non-student self excluded via applyEmail filter done in Dashboard):
 *   status IN ('pending','approving','approved') AND applyDate today
 *
 * Mirrors Android: GatePassDao.getActiveGatePassesByMember / getActiveGatePassesBySecurity
 */
export const useActiveGatePasses = () => {
  const { start, end } = todayBounds();
  const userRole  = (localStorage.getItem('userRole') || '').toLowerCase();
  const userEmail = (localStorage.getItem('userEmail') || '').toLowerCase();

  return useLiveQuery(async () => {
    if (userRole === 'security guard') {
      // Security: approved gate passes only, today
      return db.gatePasses
        .where('applyDate').between(start, end, true, true)
        .and(p => p.status === 'approved')
        .reverse()
        .sortBy('applyDate')
        .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
    }
    // Others: pending/approving/approved today, exclude own applications (unless hod/principal)
    const activeStatuses = ['pending', 'approving', 'approved'];
    return db.gatePasses
      .where('applyDate').between(start, end, true, true)
      .and(p => {
        if (!activeStatuses.includes((p.status || '').toLowerCase())) return false;
        // HOD/Principal see all passes; others don't see their own applications in the list
        if (userRole === 'hod' || userRole === 'principal') return true;
        return (p.applyEmail || '').toLowerCase() !== userEmail;
      })
      .toArray()
      .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
  }, [userRole, userEmail, start, end]) || EMPTY_ARRAY;
};

/** Inter-institutional active (same logic as regular gate pass, today) */
export const useActiveInterInstitutionalGatePasses = () => {
  const { start, end } = todayBounds();
  const userRole  = (localStorage.getItem('userRole') || '').toLowerCase();
  const userEmail = (localStorage.getItem('userEmail') || '').toLowerCase();

  return useLiveQuery(async () => {
    if (userRole === 'security guard') {
      return db.interInstitutionalGatePasses
        .where('applyDate').between(start, end, true, true)
        .and(p => p.status === 'approved')
        .toArray()
        .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
    }
    const activeStatuses = ['pending', 'approving', 'approved'];
    return db.interInstitutionalGatePasses
      .where('applyDate').between(start, end, true, true)
      .and(p => {
        if (!activeStatuses.includes((p.status || '').toLowerCase())) return false;
        if (userRole === 'hod' || userRole === 'principal') return true;
        return (p.applyEmail || '').toLowerCase() !== userEmail;
      })
      .toArray()
      .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
  }, [userRole, userEmail, start, end]) || EMPTY_ARRAY;
};

/**
 * Gate passes that are HISTORICAL (past or completed).
 * Mirrors Android: GatePassDao.getHistoricalGatePasses
 * Condition: applyDate < todayStart  OR  status NOT IN ('pending','approving','approved')
 */
export const useHistoricalGatePasses = () => {
  const { start } = todayBounds();
  return useLiveQuery(async () => {
    const activeStatuses = ['pending', 'approving', 'approved'];
    return db.gatePasses
      .filter(p =>
        (p.applyDate || '') < start ||
        !activeStatuses.includes((p.status || '').toLowerCase())
      )
      .toArray()
      .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
  }, [start]) || EMPTY_ARRAY;
};

export const useHistoricalInterInstitutionalGatePasses = () => {
  const { start } = todayBounds();
  return useLiveQuery(async () => {
    const activeStatuses = ['pending', 'approving', 'approved'];
    return db.interInstitutionalGatePasses
      .filter(p =>
        (p.applyDate || '') < start ||
        !activeStatuses.includes((p.status || '').toLowerCase())
      )
      .toArray()
      .then(arr => arr.sort((a, b) => new Date(b.applyDate) - new Date(a.applyDate)));
  }, [start]) || EMPTY_ARRAY;
};

/**
 * Visitors that are ACTIVE TODAY.
 * Mirrors Android: VisitorDao.getActiveVisitors
 * status IN ('pending','meet') AND entryDate today
 */
export const useActiveVisitors = () => {
  const { start, end } = todayBounds();
  return useLiveQuery(async () => {
    const activeStatuses = ['pending', 'meet'];
    // Use meetDate as the date field (Android uses entryDate but web stores meetDate)
    return db.visitors
      .filter(v => {
        const dateField = (v.entryDate || v.meetDate || '');
        return dateField >= start && dateField <= end &&
               activeStatuses.includes((v.status || '').toLowerCase());
      })
      .toArray()
      .then(arr => arr.sort((a, b) =>
        new Date(b.entryDate || b.meetDate) - new Date(a.entryDate || a.meetDate)
      ));
  }, [start, end]) || EMPTY_ARRAY;
};

/**
 * Visitors that are HISTORICAL.
 * Mirrors Android: VisitorDao.getHistoricalVisitors
 * entryDate < todayStart OR status NOT IN ('pending','meet')
 */
export const useHistoricalVisitors = () => {
  const { start } = todayBounds();
  return useLiveQuery(async () => {
    const activeStatuses = ['pending', 'meet'];
    return db.visitors
      .filter(v => {
        const dateField = (v.entryDate || v.meetDate || '');
        return dateField < start || !activeStatuses.includes((v.status || '').toLowerCase());
      })
      .toArray()
      .then(arr => arr.sort((a, b) =>
        new Date(b.entryDate || b.meetDate) - new Date(a.entryDate || a.meetDate)
      ));
  }, [start]) || EMPTY_ARRAY;
};

// ── Sync triggers (fire-and-forget — no await needed by callers) ──────────────
export const triggerGatePassSync = (token) =>
  GatePassRepository.syncGatePasses(token);

export const triggerInterInstitutionalGatePassSync = (token) =>
  InterInstitutionalGatePassRepository.syncInterInstitutionalGatePasses(token);

export const triggerVisitorSync = (token) =>
  VisitorRepository.syncVisitors(token);

export const triggerAllPassSync = (token) =>
  Promise.all([
    triggerGatePassSync(token),
    triggerInterInstitutionalGatePassSync(token),
    triggerVisitorSync(token),
  ]);
