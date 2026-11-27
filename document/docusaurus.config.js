// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const prismReactRenderer = require('prism-react-renderer');
const lightCodeTheme = prismReactRenderer.themes ? prismReactRenderer.themes.github : require('prism-react-renderer/themes/github');
const darkCodeTheme = prismReactRenderer.themes ? prismReactRenderer.themes.dracula : require('prism-react-renderer/themes/dracula');
const {onlineOrigins} = require('./hasor-tech/routes');
const {projectVars, replaceVariables} = require('./plugins/projectVars.js');

/** @type {import('@docusaurus/types').Config} */
const config = require('./hasor-tech/config')({
    title: 'Hasor',
    tagline: 'Hasor 框架',
    url: process.env.DOCS_URL || onlineOrigins.hasor,
    baseUrl: process.env.DOCS_BASE_URL || '/',
    onBrokenLinks: 'throw',
    markdown: {
        preprocessor: ({fileContent}) => replaceVariables(fileContent),
        // Resolve variables before extracting metadata or recognizing Markdown links.
        parseFrontMatter: ({filePath, fileContent, defaultParseFrontMatter}) => defaultParseFrontMatter({
            filePath,
            fileContent: replaceVariables(fileContent),
        }),
        hooks: {
            onBrokenMarkdownLinks: 'warn',
        },
    },
    favicon: 'img/favicon/hasor.svg',
    organizationName: 'HasorTech', // Usually your GitHub org/user name.
    projectName: 'hasor',   // Usually your repo name.

    i18n: {
        defaultLocale: 'zh-cn',
        locales: ['zh-cn', 'en'],
    },

    presets: [
        [
            'classic',
            /** @type {import('@docusaurus/preset-classic').Options} */
            ({
                docs: {
                    path: 'docs/cn/docusaurus-plugin-content-docs/current',
                    sidebarPath: require.resolve('./sidebars.js'),
                    editLocalizedFiles: true,
                    editUrl: 'https://gitee.com/hasortech/hasor/blob/main/document/',
                },
                blog: false,
                theme: {
                    customCss: require.resolve('./src/css/custom.css'),
                },
            }),
        ],
    ],

    themeConfig: /** @type {import('@docusaurus/preset-classic').ThemeConfig} */ {
        metadata: [
            {
                name: 'keywords',
                content: 'hasor,hasor-core,hasor-config,hasor-web,hasor-boot,ioc,aop,webmvc,java开源,开源项目',
            },
            {
                name: 'description',
                content: 'Hasor 是一个轻量级 Java 框架，当前核心能力由 hasor-core、hasor-config、hasor-web、hasor-boot 组成。',
            }
        ],
        colorMode: {
            disableSwitch: true,
        },
        prism: {
            theme: lightCodeTheme,
            darkTheme: darkCodeTheme,
            additionalLanguages: ['java']
        },
        footer: {
            style: 'dark',
            copyright: `Copyright © ${new Date().getFullYear()} Hasor. Built with Docusaurus.`,
        },
    },
    plugins: [
        [
            require.resolve('./plugins/llms.js'),
            {
                siteTitle: 'Hasor',
                overview: 'guides/getting-started/overview.md',
                descriptions: {
                    'zh-cn': `Hasor ${projectVars.lastReleaseVer} 文档：依赖注入、配置、Web MVC 和应用启动。`,
                    en: `Hasor ${projectVars.lastReleaseVer} documentation: dependency injection, configuration, Web MVC, and application startup.`,
                },
                depth: 2,
                onRouteError: 'throw',
                content: {
                    enableMarkdownFiles: false,
                    enableLlmsFullTxt: false,
                    includeDocs: true,
                    includeBlog: false,
                    includePages: false,
                    includeGeneratedIndex: false,
                    excludeRoutes: ['**/tags{,/**}', '**/search', '**/404.html'],
                },
            },
        ],
    ],
    themes: [
        [
            require.resolve("@easyops-cn/docusaurus-search-local"),
            /** @type {import("@easyops-cn/docusaurus-search-local").PluginOptions} */
            ({
                hashed: true,
                indexBlog: false,
                language: ["en", "zh"],
            }),
        ],
    ]
}, 'hasor');

module.exports = function createConfig() {
    if (process.env.DOCUSAURUS_CURRENT_LOCALE !== 'en') {
        return config;
    }
    const messages = require('./docs/en/code.json');
    return {
        ...config,
        tagline: messages['site.tagline'].message,
        themeConfig: {
            ...config.themeConfig,
            metadata: config.themeConfig.metadata.map((entry) => ({
                ...entry,
                content: messages['site.' + entry.name]?.message ?? entry.content,
            })),
        },
    };
};
