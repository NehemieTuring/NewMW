# 🏦 Logique Métier du Backend — Mutuelle Nehemie

## Vue d'ensemble
La **Mutuelle Nehemie** est une plateforme de gestion financière coopérative construite en **Spring Boot (Java)**. Elle gère l'épargne, les prêts, la solidarité sociale, et la redistribution des bénéfices.

---

## 🏗️ Architecture Métier

### 5 Caisses de Trésorerie
1.  **INSCRIPTION** : Reçoit les frais d'adhésion (50k). Finance les événements communautaires (Agapes).
2.  **SOLIDARITY** : Reçoit les cotisations annuelles (150k). Finance les aides sociales (maladie, décès, etc.).
3.  **SAVING** : Reçoit l'épargne des membres. Sert de source pour les prêts.
4.  **REFUELING** : Reçoit les remboursements du renflouement de fin d'exercice.
5.  **PENALTY** : Reçoit les amendes pour retard ou absence.

---

## 1. 📅 Gestion des Exercices et Sessions
- **Exercice** : Une année comptable avec des paramètres fixes (Taux d'intérêt 3%, Cotisation 150k, etc.). Un seul actif à la fois.
- **Session** : Une réunion périodique. Les opérations financières ne se font que pendant une session ouverte. Clôture automatique par tâche planifiée (Cron job).

---

## 2. 👥 Gestion des Membres
### Inscription
- Coût : **50 000 XAF**.
- Création automatique du compte utilisateur et du profil membre.
- Initialisation de la dette de solidarité.

### Statut de Conformité
Le statut est calculé dynamiquement :
- **EN_REGLE** : Aucune dette de solidarité ou de renflouement.
- **INSOLVABLE** : Dettes < 250 000 XAF.
- **INACTIF** : Dettes >= 250 000 XAF.

---

## 3. 🏦 Système de Prêts
### Conditions
- Le membre doit être **EN_REGLE**.
- Un seul prêt actif à la fois.
- **Intérêts (3%) déduits à la source** (Le membre reçoit le net mais doit rembourser le brut).

### Plafonds (Paliers dégressifs)
| Épargne | Multiplicateur |
| :--- | :--- |
| 0 - 500k | **x5** |
| 500k - 1M | **x4** |
| 1M - 1.5M | **x3** |
| 1.5M - 2M | **x2** |
| > 2M | **x1.5** |

---

## 4. 🤝 Solidarité et Aides
### Aides Sociales
- Financement à **100% par la caisse Solidarité**.
- **Compensation automatique** : Avant de verser une aide au bénéficiaire, le système prélève automatiquement ses dettes (solidarité en retard, puis prêts actifs).

---

## 5. 🔄 Renflouement et Clôture
### Renflouement (Refueling)
Mécanisme pour recharger le fonds social. La charge est répartie uniquement entre les membres présents au moment de chaque dépense sociale au cours de l'année.

### Distribution des Intérêts
En fin de session, les intérêts générés par les prêts sont redistribués à tous les membres au prorata de leur épargne.

---

## 🔐 Gouvernance
- **SECRETAIRE_GENERALE** : Gestion opérationnelle.
- **TRESORIER** : Saisie des flux financiers.
- **PRESIDENT** : Supervision et rapports.
- **SUPER_ADMIN** : Configuration système.
