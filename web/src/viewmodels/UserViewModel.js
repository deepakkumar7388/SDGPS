import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../database/db';
import { UserRepository } from '../repositories/UserRepository';

export const useUsers = () => {
  // Returns a reactive stream of all users
  const users = useLiveQuery(() => db.users.toArray(), []);
  return users;
};

export const triggerUserSync = (token) => {
  UserRepository.syncUsers(token);
};
