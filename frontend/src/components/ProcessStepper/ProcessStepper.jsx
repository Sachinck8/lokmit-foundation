import './ProcessStepper.css'

export default function ProcessStepper({ steps, tagline, taglineLabel, title, accent = false }) {
  return (
    <div className={`process-stepper${accent ? ' process-stepper--accent' : ''}`}>
      {title && (
        <h2 className="process-stepper__title">{title}</h2>
      )}
      {tagline && (
        <p className="process-stepper__tagline">{tagline}</p>
      )}
      <ol className="process-stepper__list">
        {steps.map((step, index) => (
          <li key={index} className="process-stepper__item">
            <span className="process-stepper__index" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
            <span className="process-stepper__label">{step.label}</span>
            {step.description && (
              <span className="process-stepper__description">{step.description}</span>
            )}
          </li>
        ))}
      </ol>
      {taglineLabel && (
        <p className="process-stepper__footnote">{taglineLabel}</p>
      )}
    </div>
  )
}
