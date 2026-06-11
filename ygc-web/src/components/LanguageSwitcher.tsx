import { useTranslation } from 'react-i18next';
import { supportedLocales, setLocale, type LocaleCode } from '../i18n';

export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation();
  const current = i18n.language?.slice(0, 2) as LocaleCode;

  return (
    <div className="lang-switcher">
      <small className="lang-label">{t('lang.select')}</small>
      <div className="lang-badges">
        {supportedLocales.map(({ code, label }) => (
          <button
            key={code}
            type="button"
            className={`lang-badge${current === code ? ' active' : ''}`}
            onClick={() => setLocale(code)}
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
}
