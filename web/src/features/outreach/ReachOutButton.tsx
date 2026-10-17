import { useEffect, useRef, useState } from 'react'
import type { PersonType } from '../../lib/api'
import { TYPE_LABEL } from './outreachFormat'
import { useTargetPerson } from './hooks'

/**
 * Records a reach-out from the card that prompted it.
 *
 * The whole justification for storing this at all is that it costs one click at the
 * moment of sending — the vault refuses a hand-maintained invite log because that is
 * what rotted the last system. So the form asks for the least that identifies a human
 * and takes the company and the posting from the card rather than making them be typed.
 */
export function ReachOutButton({
  companyName,
  postingId,
}: {
  companyName: string | null
  postingId: string
}) {
  const [open, setOpen] = useState(false)
  const record = useTargetPerson()
  const nameRef = useRef<HTMLInputElement>(null)

  // A company we can't name can't be filed under one.
  if (!companyName) return null

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="rounded-pill px-3.5 py-2 text-[13px] font-semibold text-text-mid shadow-[inset_0_0_0_1px_var(--color-line-strong)] transition-colors hover:bg-hover hover:text-text-hi"
      >
        reach out
      </button>

      {open ? (
        <ReachOutDialog
          companyName={companyName}
          postingId={postingId}
          nameRef={nameRef}
          pending={record.isPending}
          error={record.isError}
          onClose={() => {
            setOpen(false)
            record.reset()
          }}
          onSubmit={(person) =>
            record.mutate(
              { companyName, postingId, channel: 'LINKEDIN_INVITE', kind: 'INVITE', ...person },
              { onSuccess: () => setOpen(false) },
            )
          }
        />
      ) : null}
    </>
  )
}

interface Person {
  personName: string
  personTitle: string | null
  linkedinUrl: string | null
  email: string | null
  personType: PersonType | null
}

function ReachOutDialog({
  companyName,
  nameRef,
  pending,
  error,
  onClose,
  onSubmit,
}: {
  companyName: string
  postingId: string
  nameRef: React.RefObject<HTMLInputElement | null>
  pending: boolean
  error: boolean
  onClose: () => void
  onSubmit: (person: Person) => void
}) {
  const [name, setName] = useState('')
  const [title, setTitle] = useState('')
  const [url, setUrl] = useState('')
  const [email, setEmail] = useState('')
  const [type, setType] = useState<PersonType | ''>('')

  useEffect(() => {
    nameRef.current?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [nameRef, onClose])

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    onSubmit({
      personName: name.trim(),
      personTitle: title.trim() || null,
      linkedinUrl: url.trim() || null,
      email: email.trim() || null,
      personType: type === '' ? null : type,
    })
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center p-4">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden="true" />
      <form
        onSubmit={submit}
        role="dialog"
        aria-label={`Record a reach-out at ${companyName}`}
        className="relative w-full max-w-[420px] rounded-card border border-line bg-surface p-5 shadow-[var(--shadow-raised)]"
      >
        <h3 className="text-[17px] font-semibold tracking-tight text-text-hi">
          reach out at {companyName}
        </h3>
        <p className="mt-1 text-[13px] text-text-lo">
recorded at send time. whether they accept stays on LinkedIn.
        </p>

        <label className="mt-4 block text-[13px] text-text-mid">
          who
          <input
            ref={nameRef}
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="their name"
            className={field}
          />
        </label>

        <label className="mt-3 block text-[13px] text-text-mid">
          their title <span className="text-text-faint">(optional)</span>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="engineering manager"
            className={field}
          />
        </label>

        <label className="mt-3 block text-[13px] text-text-mid">
          what they do <span className="text-text-faint">(optional)</span>
          {/* ROUTER.md decides this before anything is drafted: it sets the register and
              the ask. A hiring manager interviews and never refers; a recruiter routes
              and has no referral concept at all. */}
          <select
            value={type}
            onChange={(e) => setType(e.target.value as PersonType | '')}
            className={field}
          >
            <option value="">—</option>
            {(Object.keys(TYPE_LABEL) as PersonType[]).map((t) => (
              <option key={t} value={t}>
                {TYPE_LABEL[t]}
              </option>
            ))}
          </select>
        </label>

        <label className="mt-3 block text-[13px] text-text-mid">
          profile <span className="text-text-faint">(optional)</span>
          <input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://linkedin.com/in/…"
            className={field}
          />
        </label>

        <label className="mt-3 block text-[13px] text-text-mid">
          email <span className="text-text-faint">(optional)</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="they@company.com"
            className={field}
          />
        </label>

        {error ? (
          <p className="mt-3 text-[13px] text-danger">couldn't save that — the agent didn't respond.</p>
        ) : null}

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-pill px-4 py-2 text-[13px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi"
          >
            cancel
          </button>
          <button
            type="submit"
            disabled={pending || !name.trim()}
            className="rounded-pill bg-mint px-4 py-2 text-[13px] font-semibold text-mint-ink transition-opacity hover:opacity-85 disabled:opacity-40"
          >
            {pending ? 'saving…' : 'record it'}
          </button>
        </div>
      </form>
    </div>
  )
}

const field =
  'mt-1 w-full rounded-control border border-line-strong bg-surface px-3 py-2 text-[14px] text-text-hi placeholder:text-text-faint focus:border-mint focus:outline-none'
