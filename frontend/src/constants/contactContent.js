import { company } from './siteIdentity.js'

export const contactContent = {
  hero: {
    title: 'Contact Us',
    subtitle:
      'Reach out for technical consultancy, project advisory, documentation support, skill development, or general enquiries.',
    background: 'linear-gradient(135deg, #0d3d21 0%, #14532d 100%)',
  },
  info: {
    name: company.name,
    address: company.registeredOffice.lines.join(' '),
    email: company.officialEmail,
    director: company.director.name,
    directorTitle: company.director.title,
  },
  enquiryCategories: [
    { value: 'consultancy', label: 'Consultancy Enquiry' },
    { value: 'project', label: 'Project Enquiry' },
    { value: 'partnership', label: 'Partnership Enquiry' },
    { value: 'csr', label: 'CSR Enquiry' },
    { value: 'employer', label: 'Employer Enquiry' },
    { value: 'career', label: 'Career Enquiry' },
    { value: 'general', label: 'General Enquiry' },
  ],
  form: {
    offlineNotice: {
      title: 'Online enquiry submission is not connected yet',
      text:
        'This form is not linked to our systems at the moment, so nothing has been sent. Please email us directly and we will respond to your enquiry.',
      ctaLabel: 'Email Us Instead',
    },
    fields: {
      name: { label: 'Full Name', placeholder: 'Your full name', required: true },
      email: { label: 'Email Address', placeholder: 'your.email@example.com', required: true },
      phone: { label: 'Phone Number', placeholder: 'Optional', required: false },
      category: { label: 'Enquiry Category', placeholder: 'Select a category', required: true },
      subject: { label: 'Subject', placeholder: 'Brief subject', required: true },
      message: { label: 'Message', placeholder: 'Tell us about your requirement...', required: true },
    },
    submit: 'Send Enquiry',
    privacyNote:
      'Your information will be used only to respond to your enquiry.',
  },
  note:
    'No phone number has been provided for public listing at this time. Please use the official email or contact form for enquiries.',
}
