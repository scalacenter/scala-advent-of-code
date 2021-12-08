// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Scala Center Advent of Code',
  tagline: '',
  url: 'https://scalacenter.github.io/',
  baseUrl: '/scala-advent-of-code/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'scalacenter',
  projectName: 'scala-advent-of-code',

  presets: [
    [
      '@docusaurus/preset-classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          path: 'target/mdoc',
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: param => `https://github.com/scalacenter/scala-advent-of-code/edit/website/docs/${param.docPath}`,
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'Scala Advent of Code',
        logo: {
          alt: 'Scala Center',
          src: '/img/scala-tree.png',
        },
        items: [
          {
            type: 'doc',
            docId: 'introduction',
            position: 'left',
            label: 'Introduction',
          },
          {
            type: 'doc',
            docId: 'setup',
            position: 'left',
            label: 'Setup',
          },
          {
            type: 'doc',
            docId: 'puzzles/day1',
            position: 'left',
            label: 'Puzzles',
          },
          {
            href: 'https://github.com/scalacenter/scala-advent-of-code',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        links: [
          {
            title: 'Content',
            items: [
              {
                label: 'Introduction',
                to: '/introduction',
              },
              {
                label: 'Setup',
                to: '/setup',
              },
              {
                label: 'Puzzles',
                to: '/puzzles/day1',
              }
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'Discord',
                href: 'https://discord.com/channels/632150470000902164/913451015246868530'
              },
              {
                label: 'Twitter',
                href: 'https://twitter.com/scala_lang',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Scala Center',
                href: 'https://scala.epfl.ch/',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/scalacenter',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Scala Center, Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages : ['java', 'scala', 'batch']
      },
    }),
    scripts: [
      {
        src: "https://plausible.scala-lang.org/js/plausible.js",
        defer: true,
        async: true,
        "data-domain": "scalacenter.github.io"
      }
    ]
};

module.exports = config;
