import { useState } from 'react'
import './Accordion.css'

export default function Accordion({ items, title, alwaysOpenFirst = false }) {
  const [openIndex, setOpenIndex] = useState(alwaysOpenFirst ? 0 : null)

  return (
    <div className="accordion">
      {title && (
        <h2 className="accordion__heading">{title}</h2>
      )}
      <ul className="accordion__list">
        {items.map((item, index) => (
          <li key={index} className="accordion__item">
            <button
              type="button"
              className={`accordion__trigger${openIndex === index ? ' accordion__trigger--open' : ''}`}
              aria-expanded={openIndex === index}
              aria-controls={`accordion-panel-${index}`}
              onClick={() => {
                setOpenIndex(openIndex === index ? (alwaysOpenFirst ? 0 : null) : index)
              }}
            >
              <span className="accordion__question">{item.question}</span>
              <svg className="accordion__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            {openIndex === index && (
              <div className="accordion__panel" id={`accordion-panel-${index}`} role="region">
                <div className="accordion__answer">
                  {Array.isArray(item.answer) ? item.answer.map((part, i) => <p key={i}>{part}</p>) : item.answer}
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
