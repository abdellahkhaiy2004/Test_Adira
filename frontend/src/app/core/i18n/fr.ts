/**
 * Every visible string of the application, in French.
 *
 * Keeping them here means adding a locale is adding a file with the same shape, not editing templates.
 */
export const FR = {
  app: {
    title: 'Banque — Espace client',
    shortTitle: 'Banque',
    nav: {
      dashboard: 'Tableau de bord',
      accounts: 'Comptes',
      transactions: 'Transactions',
      newTransaction: 'Nouvelle transaction',
      transfers: 'Virement',
      assistant: 'Assistant',
      menu: 'Menu de navigation',
      logout: 'Quitter la démonstration',
    },
  },

  common: {
    loading: 'Chargement en cours…',
    retry: 'Réessayer',
    cancel: 'Annuler',
    confirm: 'Confirmer',
    back: 'Retour',
    close: 'Fermer',
    required: 'Ce champ est obligatoire.',
    all: 'Tous',
    none: 'Aucun',
    previous: 'Précédent',
    next: 'Suivant',
    page: 'Page',
    of: 'sur',
  },

  status: {
    ACTIVE: 'Actif',
    BLOCKED: 'Bloqué',
  },

  login: {
    title: 'Authentification',
    notImplementedTitle: 'Authentification Keycloak non connectée',
    notImplementedBody:
      "Le backend n'expose aujourd'hui aucun point d'authentification et aucune de ses API n'est " +
      "protégée. Cet écran n'imite donc pas une connexion réelle : il ouvre simplement la " +
      'démonstration pour permettre la navigation.',
    notImplementedDetail:
      "Lorsque Keycloak sera branché, seul le service d'authentification de cette application " +
      'changera ; les écrans et les appels API resteront identiques.',
    enter: 'Entrer dans la démonstration',
  },

  dashboard: {
    title: 'Tableau de bord',
    subtitle: 'Vue d’ensemble des comptes et des dernières opérations',
    totals: 'Total par devise',
    totalsHint: 'Somme calculée à partir des comptes affichés ; ce n’est pas un solde fourni par la banque.',
    accounts: 'Comptes',
    recentTransactions: 'Dernières transactions',
    noAccounts: 'Aucun compte à afficher. Créez un compte depuis la page Comptes.',
    noTransactions: 'Aucune transaction enregistrée pour le moment.',
    quickActions: 'Actions rapides',
    seeAll: 'Tout afficher',
  },

  accounts: {
    title: 'Comptes',
    subtitle: 'Consulter et gérer les comptes bancaires',
    reference: 'Référence client',
    status: 'Statut',
    currency: 'Devise',
    balance: 'Solde',
    identifier: 'Identifiant',
    actions: 'Actions',
    details: 'Détails',
    empty: 'Aucun compte ne correspond à cette recherche.',
    filterLabel: 'Filtrer par référence client',
    filterPlaceholder: 'Ex. CUST-1',
    filter: 'Filtrer',
    clear: 'Effacer',
    create: 'Créer un compte',
    createTitle: 'Nouveau compte',
    openingBalance: 'Solde initial (optionnel)',
    created: 'Compte créé.',
    block: 'Bloquer',
    unblock: 'Réactiver',
    blocked: 'Compte bloqué.',
    unblocked: 'Compte réactivé.',
    confirmBlock: 'Bloquer ce compte ? Aucun débit ni crédit ne sera plus accepté.',
    confirmUnblock: 'Réactiver ce compte ?',
    detailTitle: 'Détail du compte',
    history: 'Historique du compte',
    notFound: 'Ce compte est introuvable.',
  },

  transactions: {
    title: 'Transactions',
    subtitle: 'Historique des opérations, de la plus récente à la plus ancienne',
    id: 'Identifiant',
    account: 'Compte',
    amount: 'Montant',
    currency: 'Devise',
    category: 'Catégorie marchand',
    country: 'Pays',
    date: 'Date',
    empty: 'Aucune transaction à afficher.',
    filterByAccount: 'Filtrer par compte',
    allAccounts: 'Tous les comptes',
    paginationHint:
      "L'API renvoie l'historique complet ; la pagination ci-dessous est effectuée dans le navigateur.",
    transferOut: 'Virement émis',
    transferIn: 'Virement reçu',
  },

  newTransaction: {
    title: 'Nouvelle transaction',
    subtitle: 'Enregistrer un débit sur un compte',
    account: 'Compte à débiter',
    selectAccount: 'Sélectionner un compte',
    amount: 'Montant',
    currency: 'Devise',
    category: 'Catégorie marchand',
    categoryPlaceholder: 'Ex. GROCERY',
    country: 'Pays',
    countryPlaceholder: 'Ex. MA',
    submit: 'Enregistrer la transaction',
    submitting: 'Enregistrement…',
    success: 'Transaction enregistrée.',
    successDetail: 'Identifiant de la transaction :',
    currencyWarning:
      'La devise saisie diffère de celle du compte sélectionné. La banque refusera l’opération.',
    noAccounts: 'Aucun compte disponible. Créez d’abord un compte.',
    rulesNote:
      'Le plafond de montant est défini par la banque (Config Server) et vérifié côté serveur.',
    newOne: 'Enregistrer une autre transaction',
  },

  transfers: {
    title: 'Virement',
    subtitle: 'Transférer un montant d’un compte vers un autre',
    source: 'Compte émetteur',
    destination: 'Compte bénéficiaire',
    amount: 'Montant',
    currency: 'Devise',
    country: 'Pays',
    review: 'Vérifier le virement',
    confirmTitle: 'Confirmer le virement',
    confirmBody: 'Vous êtes sur le point de transférer',
    from: 'depuis',
    to: 'vers',
    execute: 'Exécuter le virement',
    submitting: 'Exécution…',
    success: 'Virement exécuté.',
    successDetail: 'Identifiant du virement :',
    sameAccount: 'Le compte émetteur et le compte bénéficiaire doivent être différents.',
    currencyWarning:
      'Les deux comptes n’ont pas la même devise. La banque refusera le virement.',
    needTwoAccounts: 'Deux comptes au minimum sont nécessaires pour effectuer un virement.',
    newOne: 'Effectuer un autre virement',
  },

  assistant: {
    title: 'Assistant bancaire',
    subtitle: 'Posez vos questions ; l’assistant agit via les outils autorisés de la banque.',
    placeholder: 'Écrivez votre message…',
    send: 'Envoyer',
    thinking: 'L’assistant réfléchit…',
    you: 'Vous',
    assistant: 'Assistant',
    empty: 'Démarrez la conversation, par exemple : « Quels sont mes comptes ? »',
    tools: 'Outils bancaires disponibles',
    noTools:
      'Aucun outil bancaire n’est accessible : l’assistant peut discuter mais ne peut effectuer ' +
      'aucune opération. Vérifiez que les services métier et leurs serveurs MCP sont démarrés.',
    toolsUnavailable: 'La liste des outils n’a pas pu être chargée.',
    reset: 'Nouvelle conversation',
    turnFailed: 'Ce message n’a pas pu être traité.',
  },

  validation: {
    required: 'Ce champ est obligatoire.',
    amountPositive: 'Le montant doit être strictement positif.',
    amountNumber: 'Saisissez un montant numérique valide.',
    amountDecimals: 'Le montant ne peut pas comporter plus de deux décimales.',
    currencyFormat: 'La devise doit comporter trois lettres majuscules, par exemple EUR.',
  },

  errors: {
    title: 'Une erreur est survenue',
    network: 'Service indisponible. Vérifiez que la passerelle API est démarrée.',
    unknown: 'Erreur inattendue du serveur.',
    notFound: 'Ressource introuvable.',
    unauthorized: 'Session expirée. Veuillez vous reconnecter.',
    forbidden: 'Vous n’êtes pas autorisé à effectuer cette opération.',
  },

  notFound: {
    title: 'Page introuvable',
    body: 'La page demandée n’existe pas.',
    home: 'Revenir au tableau de bord',
  },
} as const;
