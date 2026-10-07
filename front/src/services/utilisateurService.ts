import api from './api'
import type { Utilisateur } from '@/types/utilisateur.ts'

export const utilisateurService = {
  async getAllUsers(): Promise<Utilisateur[]> {
    const response = await api.get<Utilisateur[]>('/utilisateurs')
    return response.data
  },
}
