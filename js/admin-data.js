/**
 * Data management functions (loading, saving, GitHub integration)
 */

const DataManager = {
  // Load data from GitHub Pages (public access)
  async loadDataFromGitHubPages() {
    const config = AppState.config;
    if (!config) return false;

    try {
      const pagesUrl = `https://${config.repoOwner}.github.io/${config.repoName}/${config.dataFileName}`;
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
        AppState.initializeDefaultData();   // now uses config
        console.log('📝 Using default data from config');
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

  // Commit Data to GitHub
  async commitToGitHub() {
    const config = AppState.config;
    if (!config) {
      UI.updateCommitStatus('❌ Configuration missing');
      return;
    }

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
        `https://api.github.com/repos/${config.repoOwner}/${config.repoName}/contents/${config.dataFileName}`,
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
      const raw = JSON.stringify(data, null, 2).replace(
        /"splitAmong": \[([^\]]+)\]/gs,
        (_, inner) => '"splitAmong": [' + (inner.match(/"[^"]+"/g) || []).join(', ') + ']'
      );
      const content = btoa(unescape(encodeURIComponent(raw)));
      const commitMessage = `Update kharcha data - ${new Date().toLocaleString()}`;

      // Send PUT request to GitHub API
      const putRes = await fetch(
        `https://api.github.com/repos/${config.repoOwner}/${config.repoName}/contents/${config.dataFileName}`,
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
        UI.updateCommitStatus('✅ Data committed successfully!');
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

  // Commit Configuration to GitHub
  async commitConfigToGitHub() {
    const config = AppState.config;
    if (!config) {
      UI.updateConfigCommitStatus('❌ Configuration missing', 'error');
      return;
    }

    const githubPAT = AppState.getPAT();

    if (!githubPAT) {
      UI.updateConfigCommitStatus('❌ Please set your PAT first', 'error');
      return;
    }

    UI.updateConfigCommitStatus('🔄 Saving config to GitHub...', 'processing');
    DOM.saveConfigBtn.disabled = true;
    DOM.saveConfigBtn.innerHTML = '<div class="loading-spinner mr-2"></div> Saving...';

    try {
      const configFileName = 'config.json';
      
      // Get current file SHA
      const getRes = await fetch(
        `https://api.github.com/repos/${config.repoOwner}/${config.repoName}/contents/${configFileName}`,
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
      } else {
        throw new Error(`Failed to get config file: ${getRes.status}`);
      }

      // Prepare updated content
      // Prepare updated content (compact inner objects to single lines)
      const raw = JSON.stringify(config, null, 2).replace(
        /\{[^{}]*\}/gs,
        m => '{' + m.slice(1, -1).replace(/\s*\n\s*/g, ' ').trim() + '}'
      );
      const content = btoa(unescape(encodeURIComponent(raw)));
      const commitMessage = `Update config - ${new Date().toLocaleString()}`;

      // Send PUT request to GitHub API
      const putRes = await fetch(
        `https://api.github.com/repos/${config.repoOwner}/${config.repoName}/contents/${configFileName}`,
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
        UI.updateConfigCommitStatus('✅ Config saved! Refresh page to see changes.', 'success');
        
        // Also ensure data structure reflects new config immediately in UI
        UI.initializeDropdowns();
        UI.initBillExemptions();
        
      } else {
        const errorData = await putRes.json();
        UI.updateConfigCommitStatus(`❌ Save failed: ${errorData.message}`, 'error');
      }
    } catch (err) {
      console.error('Config commit error:', err);
      UI.updateConfigCommitStatus('❌ Error saving config', 'error');
    } finally {
      DOM.saveConfigBtn.disabled = false;
      DOM.saveConfigBtn.innerHTML = '<i class="fas fa-cloud-upload-alt mr-2"></i> Save & Commit Config';
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
