import { useEffect, useState } from 'react'
import { getHealth } from '../../../services/health.js'
import './Home.css'

// Phase 1 placeholder home page. It also proves the frontend -> backend
// wiring by calling GET /api/v1/health through the Vite proxy.
export default function Home() {
  const [health, setHealth] = useState({
    state: 'checking',
    message: 'Checking backend connection...',
  })

  useEffect(() => {
    let active = true

    getHealth()
      .then((response) => {
        const data = response.data
        if (active) {
          setHealth({
            state: 'ok',
            message: `Backend reachable - ${data.service} (status: ${data.status})`,
          })
        }
      })
      .catch(() => {
        if (active) {
          setHealth({
            state: 'error',
            message: 'Backend unreachable - start the Spring Boot backend on port 8080.',
          })
        }
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <section className="home">
      <h1 className="home__title">LOKMIT FOUNDATION</h1>
      <p className="home__intro">
        Phase 1 skeleton. The full public website, job portal and admin panel are built in later phases.
      </p>
      <div className={`home__status home__status--${health.state}`}>{health.message}</div>
    </section>
  )
}