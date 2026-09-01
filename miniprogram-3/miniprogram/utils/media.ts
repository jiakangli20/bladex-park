import { env } from '../config/env'

export const DEFAULT_AD_IMAGE = '/assets/images/service-business.jpg'
export const ZHOUDAOHUA_AD_IMAGE = '/assets/images/ad-cover-zhoudaohua.jpg'

export const resolveMediaUrl = (value: unknown, fallback = DEFAULT_AD_IMAGE): string => {
  const url = String(value || '').trim()
  if (!url) return fallback
  if (url.startsWith('/assets/') || url.startsWith('wxfile://') || /^https?:\/\//.test(url)) return url
  const path = url.startsWith('/api/upload/') ? url.slice(4) : url
  return `${env().baseUrl}${path.startsWith('/') ? '' : '/'}${path}`
}

export const resolveAdImage = (ad: Record<string, any>): { image: string; fallbackImage: string } => {
  const fallbackImage = String(ad.title || '').trim() === '苏周到' ? ZHOUDAOHUA_AD_IMAGE : DEFAULT_AD_IMAGE
  return { image: resolveMediaUrl(ad.image, fallbackImage), fallbackImage }
}
