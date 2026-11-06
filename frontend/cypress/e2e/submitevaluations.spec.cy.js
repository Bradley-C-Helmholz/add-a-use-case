// UC-EVA-submit-evaluation 1d: the form leaves out a deactivated teammate. The teammate, section, rubric and evaluation
// calls are stubbed, so this checks what the form lists; the backend's own tests cover that it refuses an evaluation of
// a deactivated student. Run against the dev stack (backend + `npm run dev`) with `npm run cy:open`.

const teammate = (id, firstName, lastName, enabled) => ({
  id,
  username: `${firstName.toLowerCase()}@abc.edu`,
  enabled,
  roles: 'student',
  firstName,
  lastName,
  email: `${firstName.toLowerCase()}@abc.edu`,
  sectionId: 2,
  teamId: 1,
  teamName: 'Team1'
})

describe('Submit peer evaluations, with a deactivated teammate', () => {
  beforeEach(() => {
    cy.login('j.smith@abc.edu', '123456')
    cy.intercept('GET', '**/students/teams/*', {
      body: {
        flag: true,
        code: 200,
        message: 'Find students by team id successfully',
        data: [
          teammate(4, 'John', 'Smith', true),
          teammate(5, 'Eric', 'Hudson', true),
          teammate(6, 'Jerry', 'Moon', false) // Deactivated after the form could have been opened
        ]
      }
    }).as('teammates')
    cy.intercept('GET', '**/sections/*', {
      body: { flag: true, code: 200, message: 'Find section successfully', data: { sectionId: 2, rubricId: 1 } }
    }).as('section')
    cy.intercept('GET', '**/rubrics/*', {
      body: {
        flag: true,
        code: 200,
        message: 'Find rubric successfully',
        data: {
          rubricId: 1,
          criteria: [{ criterionId: 1, criterion: 'Quality of work', description: 'How good is the work? (0-10)', maxScore: 10 }]
        }
      }
    }).as('rubric')
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', {
      body: { flag: true, code: 200, message: 'Find evaluations successfully', data: [] }
    }).as('evaluations')

    cy.visit('/evaluations/submit-evaluations')
    cy.wait(['@teammates', '@section', '@rubric', '@evaluations'])
  })

  it('lists the active teammates and leaves out the deactivated one (1d2)', () => {
    cy.get('.el-table').should('contain', 'John Smith').and('contain', 'Eric Hudson')
    cy.get('.el-table').should('not.contain', 'Jerry Moon')
  })
})
