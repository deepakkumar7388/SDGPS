import Dexie from 'dexie';

export const db = new Dexie('DigitalPassDB');

db.version(3).stores({
  gatePasses: 'gatePassId, applyEmail, status, applyDate, lastUpdatedAt',
  interInstitutionalGatePasses: 'gatePassId, applyEmail, status, applyDate, destinationCampus, lastUpdatedAt',
  visitors: 'visitorId, applyEmail, status, meetDate, entryDate, lastUpdatedAt',
  users: 'email, role, department, campus, lastUpdatedAt',
  batches: 'id, department, campus',
  syncMetadata: 'collection, lastSyncTime'
});
export const wipeDatabase = async () => {
  try {
    await db.transaction('rw', db.tables, async () => {
      await Promise.all(db.tables.map(table => table.clear()));
    });
    console.log('Local database cleared successfully');
  } catch (error) {
    console.error('Failed to clear database:', error);
  }
};
