// js/admin-data.js
/**
 * Data management functions (loading, saving, GitHub integration)
 */

const DataManager = {
  // Load data from GitHub Pages (public access)
  async loadDataFromGitHubPages() {
    try {
      const pagesUrl = `https://${CONFIG.REPO_OWNER}.github.io/${CONFIG.REPO_NAME}/${CONFIG.DATA_FILE}`;
      const res = await fetch(pagesUrl);
      
      if (res.ok) {
        const loadedData = await res.json();
        AppState.setData(loadedData);
        console.log('✅ Data loaded from GitHub Pages');
        return true;
      }
    } catch (error) {
      console.log('Failed to load from GitHub Pages:', error);
    }

    // Fallback to localStorage
    return this.loadFromLocalStorage();
  },
  
  // Load from localStorage
  loadFromLocalStorage() {
    try {
      const savedData = Utils.loadFromLocalStorage('kharcha_data');
      
      if (savedData) {
        AppState.setData(savedData);
        console.log('⚠️ Using local data (GitHub Pages not available)');
        return true;
      } else {
        AppState.initializeDefaultData();
        console.log('📝 Using default data');
        return true;
      }
    } catch (error) {
      console.error('Error loading from localStorage:', error);
      AppState.initializeDefaultData();
      return false;
    }
  },
  
  // Save data to localStorage
  saveData() {
    const data = AppState.getData();
    Utils.saveToLocalStorage('kharcha_data', data);
    UI.updateDataPreview();
  },
  
  // Commit to GitHub
  async commitToGitHub() {
    const githubPAT = AppState.getPAT();
    
    if (!githubPAT) {
      UI.updateCommitStatus('❌ Please set your PAT first (valid for 5 minutes)');
      return;
    }

    UI.updateCommitStatus('🔄 Committing to GitHub...');
    DOM.commitBtn.disabled = true;
    DOM.commitBtn.innerHTML = '<div class="loading-spinner mr-2"></div> Committing...';

    try {
      // Get current file SHA
      const getRes = await fetch(
        `https://api.github.com/repos/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}/contents/${CONFIG.DATA_FILE}`, 
        {
          headers: {
            "Authorization": `token ${githubPAT}`,
            "Accept": "application/vnd.github.v3+json"
          }
        }
      );

      let sha = null;
      if (getRes.ok) {
        const getData = await getRes.json();
        sha = getData.sha;
      } else if (getRes.status === 404) {
        UI.updateCommitStatus('📝 Creating new data file...');
      } else {
        throw new Error(`Failed to get file: ${getRes.status}`);
      }

      // Prepare updated content
      const data = AppState.getData();
      const content = btoa(unescape(encodeURIComponent(JSON.stringify(data, null, 2))));
      const commitMessage = `Update kharcha data - ${new Date().toLocaleString()}`;

      // Send PUT request to GitHub API
      const putRes = await fetch(
        `https://api.github.com/repos/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}/contents/${CONFIG.DATA_FILE}`, 
        {
          method: "PUT",
          headers: {
            "Authorization": `token ${githubPAT}`,
            "Accept": "application/vnd.github.v3+json",
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            message: commitMessage,
            content: content,
            sha: sha
          })
        }
      );

      if (putRes.ok) {
        UI.updateCommitStatus('✅ Data committed successfully! GitHub Action will deploy public dashboard and transaction pages shortly.');
        localStorage.removeItem('kharcha_data');
      } else {
        const errorData = await putRes.json();
        console.error('GitHub API error:', errorData);
        UI.updateCommitStatus(`❌ Commit failed: ${errorData.message || 'Unknown error'}`);
      }
    } catch (err) {
      console.error('Commit error:', err);
      UI.updateCommitStatus('❌ An error occurred while committing data');
    } finally {
      DOM.commitBtn.disabled = false;
      DOM.commitBtn.innerHTML = '<i class="fas fa-cloud-upload-alt mr-2"></i> Commit to GitHub';
    }
  },
  
  // Set GitHub PAT with automatic expiration
  setPAT() {
    const token = prompt("Enter your GitHub Personal Access Token (with 'repo' scope):\n\nNote: This will be automatically cleared after 5 minutes for security.");
    
    if (token) {
      AppState.setPAT(token);
      const now = Date.now();
      localStorage.setItem('kharcha_pat', token);
      localStorage.setItem('kharcha_pat_time', now.toString());
      UI.updateCommitStatus('✅ PAT saved successfully. It will be automatically cleared in 5 minutes.');
      
      // Clear any existing timeout
      if (AppState.patTimeout) {
        clearTimeout(AppState.patTimeout);
      }
      
      // Set new timeout for 5 minutes
      const timeout = setTimeout(() => this.clearPAT(), 5 * 60 * 1000);
      AppState.setPATTimeout(timeout);
    }
  },
  
  // Clear PAT
  clearPAT() {
    AppState.clearPAT();
    UI.updateCommitStatus('🔒 PAT has been automatically cleared for security');
  },
  
  // Load saved PAT if exists and valid
  loadSavedPAT() {
    const storedPAT = localStorage.getItem('kharcha_pat');
    const storedPATTime = localStorage.getItem('kharcha_pat_time');
    
    if (storedPAT && storedPATTime) {
      const timeDiff = Date.now() - parseInt(storedPATTime);
      const fiveMinutes = 5 * 60 * 1000;
      
      if (timeDiff < fiveMinutes) {
        AppState.setPAT(storedPAT);
        const minutesRemaining = Math.ceil((fiveMinutes - timeDiff) / 1000 / 60);
        UI.updateCommitStatus(`🔑 PAT loaded (auto-clears in ${minutesRemaining} minute${minutesRemaining !== 1 ? 's' : ''})`);
        
        const timeout = setTimeout(() => this.clearPAT(), fiveMinutes - timeDiff);
        AppState.setPATTimeout(timeout);
      } else {
        this.clearPAT();
      }
    }
  }
};
