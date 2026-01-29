# CardDemo Migration - Sprint Review and Retrospective Templates

## Document Information

| Item | Details |
|------|---------|
| Project | CardDemo Mainframe to Cloud Migration |
| Version | 1.0 |
| Date | January 2026 |
| Reference | PROJECT_WAVE_PLANNING.md |

---

# Part 1: Sprint Review Template

## Sprint Review Meeting Agenda

### Meeting Details

| Field | Value |
|-------|-------|
| Sprint Number | Sprint [X] |
| Sprint Duration | [Start Date] - [End Date] |
| Wave | Wave [1-5]: [Wave Name] |
| Meeting Date | [Date] |
| Meeting Time | [Time] |
| Duration | 60-90 minutes |
| Facilitator | Scrum Master |
| Attendees | Product Owner, Development Team, Stakeholders |

---

### 1. Sprint Overview (5 minutes)

**Sprint Goal:**
> [State the sprint goal as defined during sprint planning]

**Sprint Theme:**
> [Describe the main focus area for this sprint]

**Wave Context:**
| Wave | Focus Area | Sprint Position |
|------|------------|-----------------|
| Wave 1 | Foundation & Infrastructure | Sprints 1-4 |
| Wave 2 | Core Services Migration | Sprints 5-10 |
| Wave 3 | Transaction Processing | Sprints 11-16 |
| Wave 4 | Reporting & Batch | Sprints 17-22 |
| Wave 5 | Cutover & Hypercare | Sprints 23-28 |

---

### 2. Sprint Metrics Summary (10 minutes)

#### Velocity and Capacity

| Metric | Planned | Actual | Variance |
|--------|---------|--------|----------|
| Story Points Committed | | | |
| Story Points Completed | | | |
| Sprint Velocity | | | |
| Team Capacity (hours) | | | |
| Actual Hours Worked | | | |

#### Burndown Analysis

```
[Insert burndown chart or describe trend]

Day 1:  ████████████████████████████████ 100%
Day 5:  ████████████████████████         75%
Day 10: ████████████████                 50%
Day 14: ████████                         25%
Day 15: ██                               5%
```

**Burndown Observations:**
- [ ] Consistent progress throughout sprint
- [ ] Early completion
- [ ] Late surge to complete work
- [ ] Scope added mid-sprint
- [ ] Blockers caused delays

---

### 3. Completed User Stories (20 minutes)

#### Stories Completed

| Story ID | Story Title | Points | Demo Ready | Acceptance Criteria Met |
|----------|-------------|--------|------------|------------------------|
| US-XXX | | | Yes/No | Yes/No |
| US-XXX | | | Yes/No | Yes/No |
| US-XXX | | | Yes/No | Yes/No |

#### Demo Checklist

For each completed story, demonstrate:
- [ ] Functionality works as specified
- [ ] UI matches design (if applicable)
- [ ] API endpoints respond correctly
- [ ] Error handling works properly
- [ ] Performance is acceptable
- [ ] Security requirements met

#### Wave-Specific Demo Items

**Wave 1 - Foundation:**
- [ ] AWS infrastructure provisioned
- [ ] Database schema created
- [ ] API Gateway configured
- [ ] CI/CD pipeline operational
- [ ] Development environment accessible

**Wave 2 - Core Services:**
- [ ] Auth Service: Login/logout/token refresh
- [ ] Customer Service: CRUD operations
- [ ] Account Service: View/activate/deactivate
- [ ] Card Service: Issue/manage cards

**Wave 3 - Transactions:**
- [ ] Transaction creation and validation
- [ ] Transaction history retrieval
- [ ] Payment processing
- [ ] Balance updates

**Wave 4 - Reporting & Batch:**
- [ ] Dashboard metrics
- [ ] Account statements
- [ ] Transaction reports
- [ ] Batch job execution

**Wave 5 - Cutover:**
- [ ] Data migration verification
- [ ] Parallel run comparison
- [ ] Performance benchmarks
- [ ] Rollback procedures tested

---

### 4. Incomplete Work (10 minutes)

#### Stories Not Completed

| Story ID | Story Title | Points | % Complete | Reason | Carryover? |
|----------|-------------|--------|------------|--------|------------|
| US-XXX | | | | | Yes/No |

#### Reasons for Incompletion

- [ ] Underestimated complexity
- [ ] Dependencies not ready
- [ ] Technical blockers
- [ ] Resource unavailability
- [ ] Scope creep
- [ ] External factors
- [ ] Other: _______________

#### Impact Assessment

| Impact Area | Description | Mitigation |
|-------------|-------------|------------|
| Timeline | | |
| Budget | | |
| Quality | | |
| Dependencies | | |

---

### 5. Technical Debt and Defects (10 minutes)

#### New Technical Debt

| ID | Description | Impact | Priority | Estimated Effort |
|----|-------------|--------|----------|------------------|
| TD-XXX | | High/Med/Low | | |

#### Defects Found

| ID | Description | Severity | Status | Sprint to Fix |
|----|-------------|----------|--------|---------------|
| DEF-XXX | | Critical/High/Med/Low | Open/Fixed | |

#### Quality Metrics

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Code Coverage | 80% | | |
| Critical Defects | 0 | | |
| High Defects | <3 | | |
| Build Success Rate | 95% | | |
| Deployment Success | 100% | | |

---

### 6. COBOL Migration Progress (10 minutes)

#### Program Migration Status

| COBOL Program | Target Service | Status | Functional Parity |
|---------------|----------------|--------|-------------------|
| COSGN00C | auth-service | Not Started / In Progress / Complete | Yes/No/Partial |
| COCRDLIC | customer-service | | |
| COCRDUPC | customer-service | | |
| COACTVWC | account-service | | |
| COACTUPC | account-service | | |
| COCRDSLC | card-service | | |
| COTRN00C | transaction-service | | |
| COTRN01C | transaction-service | | |
| COBIL00C | payment-service | | |
| CORPT00C | reporting-service | | |
| CBTRN02C | batch-service | | |
| CBACT03C | batch-service | | |

#### Data Migration Progress

| Data Source | Records | Migrated | Validated | Issues |
|-------------|---------|----------|-----------|--------|
| CUSTFILE | | | | |
| ACCTFILE | | | | |
| CARDFILE | | | | |
| TRANFILE | | | | |

---

### 7. Stakeholder Feedback (15 minutes)

#### Feedback Collection

**Product Owner Feedback:**
> [Record feedback here]

**Business Stakeholder Feedback:**
> [Record feedback here]

**Technical Stakeholder Feedback:**
> [Record feedback here]

#### Feedback Categories

| Category | Feedback Summary | Action Required |
|----------|-----------------|-----------------|
| Functionality | | |
| Usability | | |
| Performance | | |
| Documentation | | |
| Process | | |

---

### 8. Next Sprint Preview (5 minutes)

#### Planned Stories for Next Sprint

| Story ID | Story Title | Points | Priority |
|----------|-------------|--------|----------|
| US-XXX | | | |

#### Key Milestones Approaching

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| | | On Track / At Risk / Delayed |

#### Dependencies for Next Sprint

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| | | | |

---

### 9. Action Items

| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| | | | |

---

### 10. Sprint Review Sign-off

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Product Owner | | | |
| Scrum Master | | | |
| Tech Lead | | | |

---

# Part 2: Sprint Retrospective Template

## Sprint Retrospective Meeting Agenda

### Meeting Details

| Field | Value |
|-------|-------|
| Sprint Number | Sprint [X] |
| Wave | Wave [1-5]: [Wave Name] |
| Meeting Date | [Date] |
| Duration | 60-90 minutes |
| Facilitator | Scrum Master |
| Attendees | Development Team (Required), Product Owner (Optional) |

---

### 1. Set the Stage (5 minutes)

#### Safety Check

Rate your comfort level for sharing openly (1-5):
```
1 - Not comfortable at all
2 - Somewhat uncomfortable  
3 - Neutral
4 - Comfortable
5 - Very comfortable
```

Team Average: ___

#### Sprint Mood Check

How does the team feel about this sprint?

| Mood | Count |
|------|-------|
| Energized | |
| Satisfied | |
| Neutral | |
| Frustrated | |
| Exhausted | |

---

### 2. What Went Well (15 minutes)

#### Team Successes

List things that worked well this sprint:

| Category | What Went Well | Impact |
|----------|----------------|--------|
| Technical | | |
| Process | | |
| Collaboration | | |
| Quality | | |
| Communication | | |

#### Individual Shout-outs

| Team Member | Recognition |
|-------------|-------------|
| | |

#### Practices to Continue

- [ ] 
- [ ] 
- [ ] 

---

### 3. What Didn't Go Well (15 minutes)

#### Challenges Faced

| Category | Challenge | Root Cause | Impact |
|----------|-----------|------------|--------|
| Technical | | | |
| Process | | | |
| Communication | | | |
| External | | | |

#### Migration-Specific Challenges

| Challenge Area | Description | Frequency |
|----------------|-------------|-----------|
| COBOL Understanding | | Often / Sometimes / Rarely |
| Data Mapping | | |
| API Design | | |
| Testing | | |
| Environment | | |
| Dependencies | | |

#### Frustration Points

- 
- 
- 

---

### 4. What to Improve (20 minutes)

#### Improvement Ideas

| Idea | Category | Effort | Impact | Priority |
|------|----------|--------|--------|----------|
| | Technical/Process/Team | Low/Med/High | Low/Med/High | |

#### Root Cause Analysis (5 Whys)

For the top issue identified:

**Issue:** _______________

1. Why? 
2. Why? 
3. Why? 
4. Why? 
5. Why? 

**Root Cause:** _______________

#### Wave-Specific Improvements

**Wave 1 - Foundation Issues:**
- Infrastructure provisioning delays
- Environment configuration problems
- CI/CD pipeline issues
- Database schema changes

**Wave 2 - Core Services Issues:**
- Service integration challenges
- API contract mismatches
- Authentication/authorization issues
- Data validation gaps

**Wave 3 - Transaction Issues:**
- Transaction processing errors
- Concurrency problems
- Performance bottlenecks
- Data consistency issues

**Wave 4 - Reporting/Batch Issues:**
- Report accuracy problems
- Batch job failures
- Scheduling conflicts
- Data aggregation errors

**Wave 5 - Cutover Issues:**
- Data migration errors
- Parallel run discrepancies
- Rollback procedure gaps
- Communication breakdowns

---

### 5. Action Items (15 minutes)

#### Improvement Actions

| Action Item | Owner | Due Date | Success Criteria | Priority |
|-------------|-------|----------|------------------|----------|
| | | | | High/Med/Low |

#### Experiment to Try

**Experiment:** _______________

**Hypothesis:** If we [action], then [expected outcome]

**Duration:** [X] sprints

**Measurement:** _______________

#### Previous Action Items Review

| Action from Sprint [X-1] | Owner | Status | Outcome |
|--------------------------|-------|--------|---------|
| | | Done/In Progress/Not Started | |

---

### 6. Team Health Check (10 minutes)

Rate each area (1-5, where 5 is excellent):

| Health Indicator | Rating | Trend | Notes |
|------------------|--------|-------|-------|
| Team Morale | | Up/Down/Stable | |
| Collaboration | | | |
| Technical Practices | | | |
| Delivery Pace | | | |
| Code Quality | | | |
| Work-Life Balance | | | |
| Learning & Growth | | | |
| Stakeholder Relations | | | |

#### Health Trend Chart

```
Sprint:     1   2   3   4   5   6   7   8   9   10
Morale:     [_] [_] [_] [_] [_] [_] [_] [_] [_] [_]
Delivery:   [_] [_] [_] [_] [_] [_] [_] [_] [_] [_]
Quality:    [_] [_] [_] [_] [_] [_] [_] [_] [_] [_]
```

---

### 7. Migration-Specific Retrospective Questions

#### COBOL to Java Migration

1. **Knowledge Transfer:** How effective was the knowledge transfer from mainframe SMEs?
   - [ ] Excellent - All questions answered promptly
   - [ ] Good - Most questions answered
   - [ ] Fair - Some gaps in understanding
   - [ ] Poor - Significant knowledge gaps

2. **Code Complexity:** How did actual complexity compare to estimates?
   - [ ] Less complex than expected
   - [ ] As expected
   - [ ] More complex than expected
   - [ ] Much more complex than expected

3. **Testing Confidence:** How confident are we in functional parity?
   - [ ] Very confident - All scenarios tested
   - [ ] Confident - Major scenarios tested
   - [ ] Somewhat confident - Basic scenarios tested
   - [ ] Not confident - Testing gaps exist

#### Frontend Migration

1. **UI/UX Parity:** How well does the React UI match original functionality?
   - [ ] Exceeds original
   - [ ] Matches original
   - [ ] Minor gaps
   - [ ] Significant gaps

2. **User Feedback:** What feedback have we received from users?
   > [Record feedback]

#### Data Migration

1. **Data Quality:** What data quality issues were encountered?
   - [ ] None
   - [ ] Minor - easily resolved
   - [ ] Moderate - required investigation
   - [ ] Major - impacted timeline

2. **Validation Coverage:** What percentage of data was validated?
   - [ ] 100%
   - [ ] 90-99%
   - [ ] 75-89%
   - [ ] <75%

---

### 8. Retrospective Techniques Rotation

Use different techniques each sprint to keep retrospectives fresh:

| Sprint | Technique | Description |
|--------|-----------|-------------|
| 1 | Start/Stop/Continue | Classic three-column format |
| 2 | 4Ls | Liked, Learned, Lacked, Longed For |
| 3 | Sailboat | Wind (helps), Anchors (slows), Rocks (risks) |
| 4 | Mad/Sad/Glad | Emotional categorization |
| 5 | Timeline | Chronological sprint events |
| 6 | Starfish | Keep/More/Less/Stop/Start |
| 7 | DAKI | Drop/Add/Keep/Improve |
| 8 | Lean Coffee | Participant-driven agenda |
| 9 | Three Little Pigs | Straw/Sticks/Bricks (stability) |
| 10 | Appreciation | Focus on team recognition |

**This Sprint's Technique:** _______________

---

### 9. Closing (5 minutes)

#### Key Takeaways

1. 
2. 
3. 

#### One Word Summary

Each team member shares one word to describe the sprint:

| Team Member | Word |
|-------------|------|
| | |

#### Retrospective Feedback

How useful was this retrospective? (1-5): ___

Suggestions for next retrospective:
> 

---

### 10. Retrospective Sign-off

| Role | Name | Date |
|------|------|------|
| Scrum Master | | |
| Team Lead | | |

---

# Part 3: Wave Review Template

## Wave Review Meeting Agenda

Use this template at the end of each wave (every 4-6 sprints).

### Meeting Details

| Field | Value |
|-------|-------|
| Wave Number | Wave [1-5] |
| Wave Name | [Foundation/Core Services/Transactions/Reporting/Cutover] |
| Duration | [Start Date] - [End Date] |
| Meeting Date | [Date] |
| Duration | 2-3 hours |
| Attendees | Project Sponsor, Steering Committee, All Team Leads |

---

### 1. Wave Summary

#### Wave Objectives

| Objective | Status | Evidence |
|-----------|--------|----------|
| | Complete/Partial/Not Started | |

#### Wave Metrics

| Metric | Target | Actual | Variance |
|--------|--------|--------|----------|
| Total Story Points | | | |
| Sprints Completed | | | |
| Budget Spent | | | |
| Defects Found | | | |
| Defects Resolved | | | |

---

### 2. Deliverables Review

#### Wave 1 - Foundation Deliverables

| Deliverable | Status | Notes |
|-------------|--------|-------|
| AWS Infrastructure | | |
| PostgreSQL Database | | |
| API Gateway | | |
| CI/CD Pipeline | | |
| Development Environment | | |
| Security Framework | | |

#### Wave 2 - Core Services Deliverables

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Auth Service | | |
| Customer Service | | |
| Account Service | | |
| Card Service | | |
| React Frontend (Auth) | | |
| React Frontend (Customer) | | |
| React Frontend (Account) | | |
| React Frontend (Card) | | |

#### Wave 3 - Transaction Deliverables

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Transaction Service | | |
| Payment Service | | |
| React Frontend (Transactions) | | |
| React Frontend (Payments) | | |
| Transaction Validation | | |
| Payment Processing | | |

#### Wave 4 - Reporting & Batch Deliverables

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Reporting Service | | |
| Batch Service | | |
| Dashboard | | |
| Account Statements | | |
| Transaction Reports | | |
| Daily Batch Jobs | | |
| Interest Calculation | | |

#### Wave 5 - Cutover Deliverables

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Data Migration | | |
| Parallel Run | | |
| Performance Testing | | |
| UAT Sign-off | | |
| Production Deployment | | |
| Hypercare Support | | |
| Mainframe Decommission | | |

---

### 3. Risk Assessment

#### Risks Realized

| Risk ID | Description | Impact | Mitigation Applied |
|---------|-------------|--------|-------------------|
| | | | |

#### New Risks Identified

| Risk ID | Description | Probability | Impact | Mitigation Plan |
|---------|-------------|-------------|--------|-----------------|
| | | High/Med/Low | High/Med/Low | |

---

### 4. Lessons Learned

#### Technical Lessons

| Lesson | Category | Recommendation |
|--------|----------|----------------|
| | | |

#### Process Lessons

| Lesson | Category | Recommendation |
|--------|----------|----------------|
| | | |

#### Team Lessons

| Lesson | Category | Recommendation |
|--------|----------|----------------|
| | | |

---

### 5. Next Wave Planning

#### Objectives for Next Wave

| Objective | Priority | Dependencies |
|-----------|----------|--------------|
| | | |

#### Resource Adjustments

| Role | Current | Needed | Action |
|------|---------|--------|--------|
| | | | |

#### Timeline Adjustments

| Milestone | Original Date | Revised Date | Reason |
|-----------|---------------|--------------|--------|
| | | | |

---

### 6. Stakeholder Sign-off

| Role | Name | Approval | Date |
|------|------|----------|------|
| Project Sponsor | | Approved/Conditional/Not Approved | |
| Business Owner | | | |
| Technical Lead | | | |
| QA Lead | | | |

---

# Appendix A: Sprint Calendar for CardDemo Migration

## Wave 1: Foundation (Sprints 1-4)

| Sprint | Dates | Focus | Key Deliverables |
|--------|-------|-------|------------------|
| Sprint 1 | Week 1-2 | Infrastructure | AWS setup, VPC, RDS |
| Sprint 2 | Week 3-4 | Database | Schema, migrations |
| Sprint 3 | Week 5-6 | Gateway | API Gateway, routing |
| Sprint 4 | Week 7-8 | DevOps | CI/CD, monitoring |

## Wave 2: Core Services (Sprints 5-10)

| Sprint | Dates | Focus | Key Deliverables |
|--------|-------|-------|------------------|
| Sprint 5 | Week 9-10 | Auth | Login, registration, JWT |
| Sprint 6 | Week 11-12 | Customer | Customer CRUD |
| Sprint 7 | Week 13-14 | Account | Account management |
| Sprint 8 | Week 15-16 | Card | Card operations |
| Sprint 9 | Week 17-18 | Frontend Core | React auth, customer UI |
| Sprint 10 | Week 19-20 | Frontend Core | React account, card UI |

## Wave 3: Transactions (Sprints 11-16)

| Sprint | Dates | Focus | Key Deliverables |
|--------|-------|-------|------------------|
| Sprint 11 | Week 21-22 | Transactions | Transaction service |
| Sprint 12 | Week 23-24 | Payments | Payment service |
| Sprint 13 | Week 25-26 | Validation | Business rules |
| Sprint 14 | Week 27-28 | Frontend Txn | Transaction UI |
| Sprint 15 | Week 29-30 | Frontend Pay | Payment UI |
| Sprint 16 | Week 31-32 | Integration | End-to-end testing |

## Wave 4: Reporting & Batch (Sprints 17-22)

| Sprint | Dates | Focus | Key Deliverables |
|--------|-------|-------|------------------|
| Sprint 17 | Week 33-34 | Reporting | Report service |
| Sprint 18 | Week 35-36 | Dashboard | Dashboard UI |
| Sprint 19 | Week 37-38 | Statements | Account statements |
| Sprint 20 | Week 39-40 | Batch | Batch service |
| Sprint 21 | Week 41-42 | Batch Jobs | Daily processing |
| Sprint 22 | Week 43-44 | Integration | Full system testing |

## Wave 5: Cutover (Sprints 23-28)

| Sprint | Dates | Focus | Key Deliverables |
|--------|-------|-------|------------------|
| Sprint 23 | Week 45-46 | Data Migration | Data export/import |
| Sprint 24 | Week 47-48 | Validation | Data verification |
| Sprint 25 | Week 49-50 | Parallel Run | Side-by-side testing |
| Sprint 26 | Week 51-52 | UAT | User acceptance |
| Sprint 27 | Week 53-54 | Go-Live | Production cutover |
| Sprint 28 | Week 55-56 | Hypercare | Support & stabilization |

---

# Appendix B: Metrics Tracking Dashboard

## Sprint Metrics

```
Sprint Velocity Trend:
Sprint 1:  ████████████████████ 20 pts
Sprint 2:  ██████████████████████ 22 pts
Sprint 3:  ████████████████████████ 24 pts
Sprint 4:  ██████████████████████████ 26 pts
Sprint 5:  ████████████████████████████ 28 pts
```

## Quality Metrics

```
Defect Trend:
Sprint 1:  ████ 4 defects
Sprint 2:  ██████ 6 defects
Sprint 3:  ███ 3 defects
Sprint 4:  ██ 2 defects
Sprint 5:  █ 1 defect
```

## Migration Progress

```
COBOL Programs Migrated:
Wave 1:  ░░░░░░░░░░░░░░░░░░░░ 0%
Wave 2:  ████████░░░░░░░░░░░░ 40%
Wave 3:  ████████████████░░░░ 80%
Wave 4:  ██████████████████░░ 90%
Wave 5:  ████████████████████ 100%
```

---

# Appendix C: Retrospective Action Item Tracker

| Sprint | Action Item | Owner | Status | Outcome |
|--------|-------------|-------|--------|---------|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
| 8 | | | | |
| 9 | | | | |
| 10 | | | | |

---

*This template should be used consistently throughout the CardDemo migration project to ensure continuous improvement and stakeholder alignment.*
