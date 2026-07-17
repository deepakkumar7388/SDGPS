import Dexie from 'dexie';

export const db = new Dexie('DigitalPassDB');

db.version(2).stores({
  gatePasses: 'gatePassId, applyEmail, status, applyDate, lastUpdatedAt',
  interInstitutionalGatePasses: 'gatePassId, applyEmail, status, applyDate, destinationCampus, lastUpdatedAt',
  visitors: 'visitorId, applyEmail, status, meetDate, lastUpdatedAt',
  users: 'email, role, department, campus, lastUpdatedAt',
  batches: 'id, department, campus',
  syncMetadata: 'collection, lastSyncTime' // collection can be 'gatePasses', 'users', etc.
});
