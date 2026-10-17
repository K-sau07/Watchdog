import { useState } from 'react'
import type { OutreachCompany, OutreachPerson } from '../lib/api'
import { useOutreach, useFollowUps, useRecordEvent } from '../features/outreach/hooks'
import {
  STAGE_LABEL,
  TYPE_LABEL,
  channelChips,
  coverageLabel,
  owes,
  owingLabel,
  sinceLabel,
} from '../features/outreach/outreachFormat'
import { PageHead } from './AppliedPage'

/**
 * Started: the outreach board, company by company.
 *
 * A company is the unit because that is how the hunt works — a posting puts a company on
 * the radar and then a roster of people inside it get approached about it on two
 * channels. Each company splits the way the work splits: people who have not written
 * back and need chasing, then people mid-conversation who need answering.
 */
export function StartedPage() {
  const board = useOutreach()
  const followUps = useFollowUps()
  // Read once in a lazy initialiser rather than on every render. The labels here are
  // day-granularity ("3d ago"), so the 1s useNowMs tick would re-render the whole board
  // every second to change nothing.
  const [nowMs] = useState(() => Date.now())

  const companies = board.data ?? []
  const due = followUps.data ?? []

  const roster = companies.reduce(
    (n, c) => n + c.awaitingReply.length + c.inConversation.length,
    0,
  )

  return (
    <>
      <PageHead
        title="Started"
        count={companies.length}
        blurb={
          companies.length > 0
            ? `${roster} people across ${companies.length} companies`
            : 'companies you have reached out to, and where each conversation stands'
        }
      />

      {due.length > 0 ? <FollowUpQueue people={due} nowMs={nowMs} /> : null}

      {board.isError ? (
        <Panel>
          <p className="text-text-hi">couldn't load the outreach board</p>
          <p className="mt-1 text-[14px] text-text-mid">the server didn't respond. it may be starting up.</p>
          <button type="button" onClick={() => board.refetch()} className={ghostButton}>
            try again
          </button>
        </Panel>
      ) : board.isLoading ? (
        <Panel>
          <p className="text-text-mid">loading…</p>
        </Panel>
      ) : companies.length === 0 ? (
        <Panel>
          <p className="text-text-hi">nobody reached out to yet</p>
          <p className="mx-auto mt-1 max-w-[46ch] text-[14px] text-text-mid">
            pick a company off the feed and record the reach-out there. people show up here the
            moment you do, and move along on their own as they reply.
          </p>
        </Panel>
      ) : (
        <div className="flex flex-col gap-4">
          {companies.map((c) => (
            <CompanyBlock key={c.companyName} company={c} nowMs={nowMs} />
          ))}
        </div>
      )}
    </>
  )
}

/**
 * The day's chases, pinned above everything.
 *
 * This is the work that actually decays — a follow-up that needs scrolling to find is a
 * follow-up that does not get sent.
 */
function FollowUpQueue({ people, nowMs }: { people: OutreachPerson[]; nowMs: number }) {
  return (
    <section className="mb-5 overflow-hidden rounded-card border border-mint-edge bg-mint-soft">
      <header className="flex items-center gap-2 px-4 py-3">
        <h3 className="text-[15px] font-semibold tracking-tight text-text-hi">
          follow up today
        </h3>
        <span className="rounded-pill bg-surface px-2 py-0.5 text-[12px] text-text-mid">
          {people.length}
        </span>
      </header>
      <ul className="bg-surface">
        {people.map((p) => (
          <ContactRow key={p.id} person={p} nowMs={nowMs} showCompany />
        ))}
      </ul>
    </section>
  )
}

function CompanyBlock({ company, nowMs }: { company: OutreachCompany; nowMs: number }) {
  return (
    <section className="overflow-hidden rounded-card border border-line bg-surface shadow-[var(--shadow-card)]">
      <header className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-line px-4 py-3">
        <h3 className="text-[16px] font-semibold tracking-tight text-text-hi">{company.companyName}</h3>
        {company.fundingStage && company.fundingStage !== 'UNKNOWN' ? (
          <span className="rounded-pill bg-badge px-2 py-0.5 text-[11px] text-text-lo">
            {company.fundingStage.toLowerCase().replace(/_/g, ' ')}
          </span>
        ) : null}
        <span className="ml-auto text-[13px] text-text-lo">{coverageLabel(company)}</span>
      </header>

      <Group title="no reply yet" people={company.awaitingReply} nowMs={nowMs} />
      <Group title="in conversation" people={company.inConversation} nowMs={nowMs} />
    </section>
  )
}

function Group({
  title,
  people,
  nowMs,
}: {
  title: string
  people: OutreachPerson[]
  nowMs: number
}) {
  if (people.length === 0) return null
  return (
    <>
      <h4 className="border-b border-line bg-rail px-4 py-1.5 text-[11px] uppercase tracking-wide text-text-faint">
        {title} ({people.length})
      </h4>
      <ul>
        {people.map((p) => (
          <ContactRow key={p.id} person={p} nowMs={nowMs} />
        ))}
      </ul>
    </>
  )
}

function ContactRow({
  person,
  nowMs,
  showCompany = false,
}: {
  person: OutreachPerson
  nowMs: number
  showCompany?: boolean
}) {
  const record = useRecordEvent()
  const owing = owes(person)
  const label = owingLabel(owing, person)
  const chips = channelChips(person)

  return (
    <li className="flex flex-wrap items-center gap-x-3 gap-y-1.5 border-b border-line px-4 py-3 last:border-b-0">
      <div className="min-w-0 basis-[180px]">
        <p className="truncate text-[14px] font-medium text-text-hi">{person.personName}</p>
        <p className="truncate text-[12px] text-text-lo">
          {showCompany ? person.companyName : person.personTitle ?? '—'}
        </p>
      </div>

      {person.personType ? (
        <span className="rounded-pill bg-badge px-2 py-0.5 text-[11px] text-text-lo">
          {TYPE_LABEL[person.personType]}
        </span>
      ) : null}

      {/* Which doors we've knocked on. This is the "are we done emailing them" readout. */}
      <span className="flex shrink-0 gap-1">
        {chips.map((c) => (
          <span
            key={c}
            className="rounded-[6px] bg-sunken px-1.5 py-0.5 font-mono text-[11px] text-text-mid"
          >
            {c}
          </span>
        ))}
      </span>

      <span className="shrink-0 text-[12px] text-text-faint">
        {sinceLabel(person.lastOutboundAt ?? person.reachedOutAt, nowMs)}
      </span>

      {person.turns > 0 ? (
        <span className="shrink-0 text-[12px] text-text-lo">
          {person.turns} {person.turns === 1 ? 'turn' : 'turns'}
        </span>
      ) : null}

      <span className="shrink-0 rounded-pill bg-badge px-2 py-0.5 text-[11px] text-text-mid">
        {STAGE_LABEL[person.stage]}
      </span>

      {label ? (
        <span
          className={[
            'ml-auto shrink-0 rounded-pill px-2.5 py-1 text-[12px]',
            owing === 'chase'
              ? 'bg-mint-soft font-semibold text-mint-ink'
              : owing === 'you'
                ? 'bg-panel font-medium text-on-panel'
                : owing === 'stop'
                  ? 'bg-danger-soft text-text-mid'
                  : 'text-text-lo',
          ].join(' ')}
        >
          {label}
        </span>
      ) : null}

      {/* Logging a reply is the one action worth having inline: it is what flips a row
          out of the chase queue, and it happens while you are looking at the row. */}
      {person.stage !== 'DEAD' && person.stage !== 'WON' ? (
        <button
          type="button"
          disabled={record.isPending}
          onClick={() =>
            record.mutate({
              id: person.id,
              direction: 'INBOUND',
              channel: person.followUpDueOn === 'EMAIL' ? 'EMAIL' : 'LINKEDIN_DM',
              kind: 'REPLY',
            })
          }
          className="shrink-0 rounded-pill border border-line-strong px-3 py-1 text-[12px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi disabled:opacity-40"
        >
          they replied
        </button>
      ) : null}

      {person.linkedinUrl ? (
        <a
          href={person.linkedinUrl}
          target="_blank"
          rel="noreferrer"
          className="shrink-0 rounded-pill border border-line-strong px-3 py-1 text-[12px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi"
        >
          open
        </a>
      ) : null}
    </li>
  )
}

function Panel({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-card border border-line bg-surface p-8 text-center shadow-[var(--shadow-card)]">
      {children}
    </div>
  )
}

const ghostButton =
  'mt-4 rounded-pill border border-line-strong px-4 py-1.5 text-[13px] text-text-mid transition-colors hover:bg-hover hover:text-text-hi'
