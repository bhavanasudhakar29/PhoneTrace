# PhoneTrace – Used Phone History Verification System

**OOPs + DBMS Integrated Project** | Java · MySQL · JDBC

## Problem Statement

Buyers of second-hand mobile phones have no reliable way to verify a device's past before purchasing. Stolen phones, frequently resold devices, and phones repaired with non-genuine parts are sold as "good condition" on platforms like OLX and in local shops. PhoneTrace is a database-driven application that maintains an IMEI-based history of every registered phone, including ownership transfers, repair records, and theft reports. Before buying, a user enters the IMEI and receives a complete history report with automatically detected red flags and a trust score.

---

## 1. Setup (about 10 minutes, once per computer)

**You need:** Java JDK (17 or newer), MySQL 8 with MySQL Workbench, and the MySQL Connector/J driver jar.

1. **Create the database.** In MySQL Workbench, open `sql/schema.sql` (File, Open SQL Script) and click the lightning bolt. It creates the database, the 5 tables, the sample data, and the login the Java code uses. No password editing needed.
2. **Add the driver.** Download MySQL Connector/J from https://dev.mysql.com/downloads/connector/j/ (choose "Platform Independent", ZIP), unzip it, and copy the `mysql-connector-j-x.x.x.jar` file into the `lib` folder.
3. **Run it.** Double-click `run-web.bat` for the website, or `run-console.bat` for the console menu. Each one compiles and starts the program for you.

### Run from the command line instead (inside the `src` folder)

Windows:
```
javac -cp ".;..\lib\*" *.java
java -cp ".;..\lib\*" WebServer
```

Mac / Linux:
```
javac -cp ".:../lib/*" *.java
java -cp ".:../lib/*" WebServer
```

Use `Main` instead of `WebServer` for the console app.

If you see `No suitable driver found`, the jar is not in `lib`. If you see `Access denied`, run `schema.sql` again in Workbench.

### Run the 3D web version

The project also has a web front end (an X-ray style 3D scanner) that uses the exact same backend. To start it,
double-click `run-web.bat` (or run `WebServer` as above). Your browser opens `http://localhost:8080` automatically (if not, type that address yourself). Keep the cmd window open while using the page. Three.js is bundled inside the `web` folder, so it works without internet.

The page has two screens. **Check a phone** is the 3D scanner. **Manage records** lets you do everything the console can: register phones, add and edit owners, transfer ownership, log repairs, report thefts and mark them recovered, and delete records. It uses the same DAO classes, so the same validation, transactions and protected-history rules apply. **Reset sample data** (bottom of the left menu) restores the 4 sample phones in one click, which is handy right before a demo.

You can also run the console app (`Main`) in a second cmd window at the same time. Both use the same database, so a change made in the console (for example, reporting a phone as stolen) shows up the next time you scan it in the browser.

### Or run in an IDE

- **IntelliJ:** Open the project folder → mark `src` as Sources Root → File › Project Structure › Libraries › `+` › Java → pick the jar → run `Main`.
- **Eclipse:** New Java Project → copy the `.java` files into `src` → right-click project › Build Path › Add External Archives → pick the jar → run `Main`.
- **VS Code:** Open the folder → in the "Java Projects" panel, click `+` next to Referenced Libraries → pick the jar → run `Main`.

If you see `No suitable driver found`, the jar is not on the classpath. If you see `Access denied`, the password in `DBConnection.java` is wrong.

---

## 2. Sample IMEIs for the demo

| IMEI | Phone | What the report shows |
|---|---|---|
| `356789012345671` | Samsung Galaxy S23 | Clean history → **SAFE TO BUY** (100) |
| `356789012345672` | Apple iPhone 13 | 3 owners in a year, display replaced twice with fake parts → **AVOID** (20) |
| `356789012345673` | OnePlus 11 | Active theft report → **DO NOT BUY – REPORTED STOLEN** |
| `356789012345674` | Redmi Note 12 | Was stolen but RECOVERED → no theft flag |

For the iPhone, enter seller number `9840011111` (not the real owner) to also trigger the seller-mismatch flag. Owner **Ravi (ID 8)** owns nothing yet, so use him for live ownership transfers.

---

## 3. Project structure (what each file does)

**Database connectivity**
- `DBConnection` – creates one shared MySQL connection for the whole app.

**Model classes (one per table, encapsulation)**
- `Phone`, `Owner`, `Repair`, `TheftReport` – private fields with validating setters.
- `Ownership` – one ownership period; read-only because history should never be edited.
- `PhoneHistory` – bundles everything about one phone into one object.

**Data access (CRUD)**
- `CrudOperations` – interface: `add`, `getById`, `getAll`, `update`, `delete`.
- `PhoneDAO`, `OwnerDAO`, `RepairDAO`, `TheftReportDAO` – implement the interface with PreparedStatements.
- `OwnershipDAO` – the two transactions: registering a phone and transferring ownership.

**Red flag engine (abstraction, inheritance, polymorphism)**
- `RedFlagRule` – abstract parent class.
- `StolenRule`, `FrequentOwnerRule`, `RepeatedRepairRule`, `FakePartRule`, `SellerMismatchRule` – child classes, one per red flag.
- `RiskAnalyzer` – runs every rule and calculates the trust score.
- `AnalysisResult` – holds the score, the flags found and the verdict.

**Output and app**
- `WebServer` – serves the 3D web page and answers it with JSON, using Java's built-in HTTP server.
- `RegistryApi` – the add / edit / delete / transfer / reset actions for the "Manage records" screen, built on the same DAOs.
- `web/index.html` – the web page: a Three.js X-ray scanner and an animated case file.
- `HistoryService` – gathers a phone's history from all tables.
- `HistoryReport` – prints the formatted report.
- `Main` – the console menu.

---

## 4. Where each required concept is used

### OOP
| Concept | Where | One-line explanation |
|---|---|---|
| **Encapsulation** | `Phone`, `Owner`, `Repair`, `TheftReport` | Fields are private and setters validate data, so an invalid object (like a 14-digit IMEI) can never exist. |
| **Abstraction** | `RedFlagRule` (abstract class) | It defines *what* every rule must do (`isTriggered`, `getPenalty`, `getMessage`) without saying *how*. |
| **Inheritance** | The 5 rule classes `extend RedFlagRule` | Each rule inherits the rule name and the default `isCritical()` from the parent. |
| **Polymorphism** | `RiskAnalyzer.analyze()` | It loops through a `List<RedFlagRule>` and calls the same methods on each one; each rule responds differently. `StolenRule` also *overrides* `isCritical()`. |
| **Interface** | `CrudOperations<T>` | Every DAO follows the same CRUD contract, whatever the table. |

### Database design
- **5 tables**, normalized to 3NF: owner details are stored once in `owners` and referenced by ID.
- **Primary keys:** IMEI is used as the natural primary key of `phones`, since it is unique worldwide.
- **Foreign keys + one-to-many relationships:** one phone has many ownerships, repairs and theft reports.
- **Referential actions:** `ON DELETE CASCADE` (deleting a phone removes its history) and `ON DELETE RESTRICT` (an owner who appears in history can't be deleted).
- **Constraints:** `CHECK`, `UNIQUE`, `NOT NULL` – the database itself rejects bad data.

### Database connectivity
- JDBC with a single shared connection (`DBConnection`).
- **PreparedStatement** everywhere, which prevents SQL injection.
- **Transactions** (`commit` / `rollback`) in `OwnershipDAO` for multi-step operations.
- A **JOIN** query fetches ownership history with owner names.

### CRUD
| Operation | Real action |
|---|---|
| Create | Register phone, add owner, add repair, report theft |
| Read | IMEI history search, view lists |
| Update | Transfer ownership, edit phone/owner/repair, mark theft RECOVERED |
| Delete | Delete phone, owner, or repair record |

---

## 5. Demo script (about 3 minutes)

1. **Clean phone:** Menu 1 → `356789012345671` → Enter. *"A normal phone: one owner, genuine repair, score 100."*
2. **Suspicious phone:** Menu 1 → `356789012345672` → seller `9840011111`. *"Red flags appear one by one: frequent owners, repeated display repairs, fake parts, and the seller isn't the owner."*
3. **Stolen phone (the finale):** Menu 1 → `356789012345673`. *"DO NOT BUY – reported stolen."*
4. **Transaction:** Menu 4 → try transferring `356789012345673` → it's **blocked** because it's stolen. Then transfer `356789012345671` to Ravi (ID 8) → success. Search it again to show the new owner in the history.
5. **Live report + CRUD:** Menu 6 → report `356789012345671` as stolen → search it → verdict changes instantly. Then menu 6 → mark it RECOVERED.
6. **Update/Delete:** Menu 5 → update a repair; Menu 3 → try deleting Karthik (ID 1) → **blocked** by `ON DELETE RESTRICT`. Explain why.

---

## 6. Likely viva questions

**Why is IMEI the primary key?**
Every phone has a unique 15-digit IMEI worldwide, so it's a natural key. No need to create an artificial ID.

**Why a separate ownerships table instead of an owner column in phones?**
One phone has many owners over time. A single column could only store the current owner and would lose the history, and the history is the whole point of the project.

**What is a transaction and where did you use it?**
A group of SQL statements that either all succeed or all fail. Transferring ownership closes the old owner's record and adds the new one. If the second step fails, rollback undoes the first, so a phone never has zero or two current owners.

**Why PreparedStatement and not Statement?**
PreparedStatement sends user input as data, not as part of the SQL command, so it prevents SQL injection. It's also cleaner to write.

**Where is polymorphism?**
`RiskAnalyzer` stores all rules as `RedFlagRule` references and calls `isTriggered()` on each. Java decides at runtime which child class's version runs. That's runtime polymorphism (method overriding).

**Abstract class vs interface – why use both?**
`RedFlagRule` is an abstract class because the rules share common data (the rule name) and a default method (`isCritical`). `CrudOperations` is an interface because the DAOs only share a contract of method names, not any data.

**How would you add a new red flag?**
Write one new class that extends `RedFlagRule` and add it to the list in `RiskAnalyzer`. No other code changes. That's the open/closed principle.

**How does the web page connect to Java?**
`WebServer` uses the HTTP server built into the JDK. The browser calls `/api/history?imei=...`, the server runs the same `HistoryService` and `RiskAnalyzer` the console uses, and sends the result back as JSON. The backend didn't change at all, which shows the benefit of keeping layers separate.

**Why can't you delete an owner?**
If an owner appears in ownership history, `ON DELETE RESTRICT` blocks the delete, because erasing them would break the phone's history trail.

**What does CASCADE do here?**
Deleting a phone automatically deletes its ownerships, repairs and theft reports, so no orphan rows are left behind.

**Why is Ownership read-only (no setters)?**
It's an audit trail. History should only ever be added to through transfers, never edited.

---

## 7. Future scope
- A web or GUI front end, and login for repair shops and police.
- Integration with the government CEIR stolen-phone database.
- Photo upload of purchase bills, and SMS alerts to the owner when their IMEI is searched.
