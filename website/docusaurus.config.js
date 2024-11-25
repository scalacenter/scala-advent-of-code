// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const fs = require('fs');

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const math = require('remark-math');
const katex = require('rehype-katex');

const puzzlePage = /(day(\d+))\.md/;

const buildDropdown = (dir) => {
  const days = fs.readdirSync(`target/mdoc/${dir}`).map((day, i) => {
    const ns = puzzlePage.exec(day);
    const id = (
      (ns === null) ? `<unknown:'${day}'>` : `${dir}/${ns[1]}`
    );
    const n = (
      (ns === null) ? -1 : parseInt(ns[2])
    );
    return ({
      type: 'doc',
      docId: id,
      label: `Day ${n}`,
      n
    })
  });
  const sorted = days.sort((a, b) => a.n - b.n);
  return sorted
};


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
            label: 'Puzzles 2024',
            items: buildDropdown('2024/puzzles')
          },
          {
            type: 'dropdown',
            position: 'left',
            label: 'Puzzles 2023',
            items: buildDropdown('2023/puzzles')
          },
          {
            type: 'dropdown',
            position: 'left',
            label: 'Puzzles 2022',
            items: buildDropdown('2022/puzzles')
          },
          {
            type: 'dropdown',
            position: 'left',
            label: 'Puzzles 2021',
            items: buildDropdown('puzzles')
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
                label: 'Puzzles 2023',
                to: '2023/puzzles/day01',
              },
              {
                label: 'Puzzles 2022',
                to: '2022/puzzles/day01',
              },
              {
                label: 'Puzzles 2021',
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
