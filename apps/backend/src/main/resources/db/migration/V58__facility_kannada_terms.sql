-- Exact Kannada terms to accompany the baseline English hygiene policy.
-- Administrators can save a facility-specific Kannada translation in the editor.
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS terms_and_conditions_kn VARCHAR(3000);
UPDATE facilities
SET terms_and_conditions_kn = 'ಗುಟ್ಕಾ, ಸಿಗರೇಟ್, ಧೂಮಪಾನ ಮತ್ತು ತಂಬಾಕು ಬಳಕೆಯನ್ನು ನಿಷೇಧಿಸಲಾಗಿದೆ. ಸೌಲಭ್ಯದ ನಿರ್ವಾಹಕರ ಸುರಕ್ಷತೆ ಮತ್ತು ಸ್ವಚ್ಛತಾ ಸೂಚನೆಗಳನ್ನು ಪಾಲಿಸಿ.'
WHERE terms_and_conditions_kn IS NULL
  AND terms_and_conditions = 'Gutka, cigarettes, smoking and tobacco are not permitted. Follow the safety and hygiene instructions of the facility operator.';
