// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const math = require('remark-math');
const katex = require('rehype-katex');

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
          remarkPlugins: [math],
          rehypePlugins: [katex],
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  stylesheets: [
    {
      href: 'https://cdn.jsdelivr.net/npm/katex@0.13.11/dist/katex.min.css',
      integrity:
        'sha384-Um5gpz1odJg5Z4HAmzPtgZKdTBHZdw8S29IecapCSB31ligYPhHQZMIlWLYQGVoc',
      crossorigin: 'anonymous',
    },
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
            type: 'dropdown',
            position: 'left',
            label: 'Puzzles',
            items: [
              { type: 'doc', label: 'Day 1', docId: 'puzzles/day1' },
              { type: 'doc', label: 'Day 2', docId: 'puzzles/day2' },
              { type: 'doc', label: 'Day 3', docId: 'puzzles/day3' },
              { type: 'doc', label: 'Day 4', docId: 'puzzles/day4' },
              { type: 'doc', label: 'Day 5', docId: 'puzzles/day5' },
              { type: 'doc', label: 'Day 6', docId: 'puzzles/day6' },
              { type: 'doc', label: 'Day 7', docId: 'puzzles/day7' },
              { type: 'doc', label: 'Day 8', docId: 'puzzles/day8' },
              { type: 'doc', label: 'Day 9', docId: 'puzzles/day9' },
              { type: 'doc', label: 'Day 10', docId: 'puzzles/day10' },
              { type: 'doc', label: 'Day 11', docId: 'puzzles/day11' },
              { type: 'doc', label: 'Day 12', docId: 'puzzles/day12' },
              { type: 'doc', label: 'Day 13', docId: 'puzzles/day13' },
              { type: 'doc', label: 'Day 14', docId: 'puzzles/day14' },
              { type: 'doc', label: 'Day 15', docId: 'puzzles/day15' },
              { type: 'doc', label: 'Day 15', docId: 'puzzles/day16' }
            ]
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
        src: "https://plausible.scala-lang.org/js/script.js",
        defer: true,
        async: true,
        "data-domain": "scalacenter.github.io"
      }
    ]
};

module.exports = config;
