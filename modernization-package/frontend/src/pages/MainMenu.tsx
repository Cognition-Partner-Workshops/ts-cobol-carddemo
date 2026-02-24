import { useNavigate } from 'react-router-dom'

/**
 * Main Menu page - replaces the legacy COMEN01C main menu screen.
 * PF3 from any transaction screen returns here.
 */
function MainMenu() {
  const navigate = useNavigate()

  return (
    <div className="terminal-screen">
      <div className="terminal-header">
        CardDemo - Main Menu
      </div>

      <div style={{ padding: '40px 20px', textAlign: 'center' }}>
        <p style={{ color: '#ffff00', marginBottom: '30px', fontSize: '16px' }}>
          Transaction Processing Module
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', alignItems: 'center' }}>
          <button
            onClick={() => navigate('/transactions')}
            style={{ width: '350px', padding: '12px', fontSize: '15px' }}
          >
            1. List Transactions (CT00)
          </button>
          <button
            onClick={() => navigate('/transactions/view')}
            style={{ width: '350px', padding: '12px', fontSize: '15px' }}
          >
            2. View Transaction (CT01)
          </button>
          <button
            onClick={() => navigate('/transactions/add')}
            style={{ width: '350px', padding: '12px', fontSize: '15px' }}
          >
            3. Add Transaction (CT02)
          </button>
        </div>

        <p style={{ marginTop: '40px', color: '#666', fontSize: '12px' }}>
          Modernized from COBOL/CICS — Java 21 / Spring Boot 3 / React / PostgreSQL
        </p>
      </div>
    </div>
  )
}

export default MainMenu
