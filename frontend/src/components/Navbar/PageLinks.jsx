import { site } from '../../constants/siteIdentity.js'

export const primaryPageLinks = site.primaryNav

export const secondaryPageLinks = site.secondaryNav

export const loginLinks = site.loginNav

export const allPageLinks = [...primaryPageLinks, ...secondaryPageLinks]
