// UC-STU-deactivate-student: the Deactivate action in the student edit form. The student calls are stubbed, so these tests
// check what the form shows and what it sends; the backend's own tests cover who may deactivate whom and what follows.
// Run against the dev stack (backend + `npm run dev`) with `npm run cy:open`.

const john = {
  id: 4,
  username: 'j.smith@abc.edu',
  enabled: true,
  roles: 'student',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  sectionId: 2,
  sectionName: '2023-2024',
  teamId: 1,
  teamName: 'Team1'
}

const studentPage = {
  flag: true,
  code: 200,
  message: 'Find students successfully',
  data: { content: [john], number: 0, size: 10, totalElements: 1, totalPages: 1 }
}

describe('Deactivate a student', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', { body: studentPage }).as('search')
    cy.intercept('GET', '**/students/4', {
      body: { flag: true, code: 200, message: 'Find student successfully', data: john }
    }).as('student')
    cy.intercept('POST', '**/students/4/deactivation', {
      body: {
        flag: true,
        code: 200,
        message: 'Deactivate student successfully',
        data: { ...john, enabled: false }
      }
    }).as('deactivation')

    cy.visit('/students')
    cy.wait('@search')
    cy.get('[data-cy=student-edit]').first().click()
    cy.wait('@student')
  })

  it('alerts of the consequences, teammates included, and asks for confirmation (steps 3 and 4)', () => {
    cy.get('[data-cy=student-deactivate]').click()

    cy.get('.deactivate-student-dialog')
      .should('contain', 'John Smith')
      .and('contain', 'no longer be able to log in')
      .and('contain', 'Her teammates will no longer be able to submit new peer evaluations of her')
      .and('contain', 'previously submitted work are kept')
      .and('contain', 'can be reactivated later')
    cy.get('.deactivate-student-confirm').should('be.visible')
    cy.get('@deactivation.all').should('have.length', 0) // Nothing is sent before she is confirmed
  })

  it('sends nothing and leaves the student active when the actor cancels (5a)', () => {
    cy.get('[data-cy=student-deactivate]').click()

    cy.get('.deactivate-student-cancel').click()

    cy.get('.deactivate-student-dialog').should('not.exist')
    cy.get('@deactivation.all').should('have.length', 0)
    cy.get('[data-cy=student-deactivate]').should('be.visible') // She is still active, so the action is still offered
  })

  it('deactivates the student after confirmation and tells the actor (steps 5 and 6)', () => {
    cy.get('[data-cy=student-deactivate]').click()

    cy.get('.deactivate-student-confirm').click()

    cy.wait('@deactivation').its('request.url').should('match', /\/students\/4\/deactivation$/)
    cy.contains('Student deactivated successfully').should('be.visible')
    cy.get('@deactivation.all').should('have.length', 1)
  })
})
