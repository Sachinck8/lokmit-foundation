import { useEffect, useRef, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import {
  listMyResumes,
  uploadMyResume,
  downloadMyResume,
  deleteMyResume,
} from '../../services/candidateService.js'
import { candidateContent } from '../../constants/candidateContent.js'

const MAX_UPLOAD_BYTES = 5 * 1024 * 1024 // 5 MiB — mirrors the backend limit
const ACCEPTED_EXTENSIONS = '.pdf,.doc,.docx'

function formatBytes(bytes) {
  if (bytes == null) return ''
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

/**
 * A10 "My Resume" page over the existing A7.6 APIs: list (A10 new read
 * endpoint), upload/replace (A7.6.3), download (A7.6.4), delete (A7.6.5).
 * All authorization is backend-side; the page only renders what the APIs
 * return and never sees storage keys or blob internals.
 */
export default function CandidateResumes() {
  const [resumes, setResumes] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [uploadSuccess, setUploadSuccess] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [confirmingDeleteId, setConfirmingDeleteId] = useState(null)
  const [pageError, setPageError] = useState(null)
  const fileInputRef = useRef(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError(false)
    listMyResumes()
      .then(data => {
        if (active) setResumes(data)
      })
      .catch(() => {
        if (active) setLoadError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [reloadKey])

  async function handleUpload(event) {
    event.preventDefault()
    const file = fileInputRef.current && fileInputRef.current.files
      && fileInputRef.current.files[0]
    if (!file) {
      setUploadError('Please choose a file to upload.')
      return
    }
    if (file.size > MAX_UPLOAD_BYTES) {
      setUploadError('This file is larger than the 5 MB limit. Please upload a smaller file.')
      return
    }
    setUploading(true)
    setUploadError(null)
    setUploadSuccess(null)
    try {
      await uploadMyResume(file)
      setUploadSuccess('Resume uploaded. It is now your active resume.')
      if (fileInputRef.current) fileInputRef.current.value = ''
      setReloadKey(key => key + 1)
    } catch (error) {
      const status = error && error.response && error.response.status
      const apiErrors = error && error.response && error.response.data
        && error.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null
      if (status === 413) {
        setUploadError('This file is larger than the 5 MB limit.')
      } else if (firstMessage) {
        setUploadError(firstMessage)
      } else {
        setUploadError('The upload could not be completed. Please try again.')
      }
    } finally {
      setUploading(false)
    }
  }

  async function handleDownload(resume) {
    setPageError(null)
    try {
      const { blob, filename } = await downloadMyResume(resume.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename || 'resume'
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch {
      setPageError('Your resume could not be downloaded right now. Please try again.')
    }
  }

  async function handleConfirmDelete(resume) {
    setDeletingId(resume.id)
    setPageError(null)
    try {
      await deleteMyResume(resume.id)
      setConfirmingDeleteId(null)
      setReloadKey(key => key + 1)
    } catch {
      setPageError('The resume could not be deleted right now. Please try again.')
    } finally {
      setDeletingId(null)
    }
  }

  const activeResume = Array.isArray(resumes) ? resumes.find(r => r.active) : null

  return (
    <Container>
      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">My resume</h1>
        <p className="candidate-portal__muted">
          Your most recent upload is your active resume and is used with your
          applications. Uploading a new resume keeps previous versions as history.
        </p>

        {loading && (
          <div aria-hidden="true">
            <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
            <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          </div>
        )}

        {!loading && loadError && (
          <>
            <p className="candidate-portal__error" role="alert">
              We could not load your resumes right now. Please try again.
            </p>
            <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
              Try again
            </Button>
          </>
        )}

        {!loading && !loadError && Array.isArray(resumes) && resumes.length === 0 && (
          <p className="candidate-portal__muted">
            <strong>{candidateContent.empty.resumes.title}.</strong>{' '}
            {candidateContent.empty.resumes.text}
          </p>
        )}

        {!loading && !loadError && Array.isArray(resumes) && resumes.length > 0 && (
          <ul className="candidate-portal__list">
            {resumes.map(resume => (
              <li key={resume.id} className="candidate-portal__list-item">
                <div className="candidate-portal__row">
                  <div>
                    <strong>{resume.fileName}</strong>
                    <div className="candidate-portal__muted">
                      Uploaded {formatDateTime(resume.createdAt)}
                      {resume.fileSizeBytes != null && <> · {formatBytes(resume.fileSizeBytes)}</>}
                      {' · '}
                      {resume.active ? 'Active' : 'Previous version'}
                    </div>
                  </div>
                  <div className="candidate-portal__actions-row">
                    {resume.active && (
                      <span className="candidate-portal__status candidate-portal__status--SHORTLISTED">
                        Active
                      </span>
                    )}
                    {resume.active && (
                      <Button variant="outline" size="small" onClick={() => handleDownload(resume)}>
                        Download
                      </Button>
                    )}
                    {resume.active && confirmingDeleteId !== resume.id && (
                      <Button variant="ghost" size="small" onClick={() => setConfirmingDeleteId(resume.id)}>
                        Delete
                      </Button>
                    )}
                    {confirmingDeleteId === resume.id && (
                      <>
                        <span className="candidate-portal__muted">Delete this resume permanently?</span>
                        <Button
                          variant="primary"
                          size="small"
                          disabled={deletingId === resume.id}
                          onClick={() => handleConfirmDelete(resume)}
                        >
                          {deletingId === resume.id ? 'Deleting…' : 'Yes, delete'}
                        </Button>
                        <Button
                          variant="ghost"
                          size="small"
                          disabled={deletingId === resume.id}
                          onClick={() => setConfirmingDeleteId(null)}
                        >
                          Cancel
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}

        {pageError && <p className="candidate-portal__error" role="alert">{pageError}</p>}
      </div>

      <div className="candidate-portal__panel">
        <h2 className="candidate-portal__panel-title">
          {activeResume ? 'Replace active resume' : 'Upload your resume'}
        </h2>
        <form className="candidate-portal__form" onSubmit={handleUpload}>
          <div>
            <label className="candidate-portal__label" htmlFor="resume-file">Resume file</label>
            <input
              id="resume-file"
              className="candidate-portal__input"
              type="file"
              accept={ACCEPTED_EXTENSIONS}
              ref={fileInputRef}
              disabled={uploading}
            />
            <p className="candidate-portal__hint">
              PDF, DOC or DOCX up to 5 MB. The file content is verified by the
              server — the file type you see is only a filter.
            </p>
          </div>

          {uploadError && <p className="candidate-portal__error" role="alert">{uploadError}</p>}
          {uploadSuccess && <p className="candidate-portal__success" role="status">{uploadSuccess}</p>}

          <div className="candidate-portal__actions-row">
            <Button variant="primary" size="medium" type="submit" disabled={uploading}>
              {uploading ? 'Uploading…' : 'Upload resume'}
            </Button>
          </div>
        </form>
      </div>
    </Container>
  )
}
