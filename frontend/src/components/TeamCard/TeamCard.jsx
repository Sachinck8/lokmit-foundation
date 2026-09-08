export default function TeamCard({ name, role, bio, initials }) {
  const computedInitials = initials ?? name.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase()

  return (
    <article className="team-card">
      <div className="team-card__header">
        <div className="team-card__avatar" aria-hidden="true">{computedInitials}</div>
        <div className="team-card__identity">
          <h3 className="team-card__name">{name}</h3>
          <p className="team-card__role">{role}</p>
        </div>
      </div>
      {bio && (
        <p className="team-card__bio">{bio}</p>
      )}
    </article>
  )
}
